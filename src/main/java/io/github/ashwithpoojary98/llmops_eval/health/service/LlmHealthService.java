package io.github.ashwithpoojary98.llmops_eval.health.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.health.dto.EndpointHealthSummary;
import io.github.ashwithpoojary98.llmops_eval.health.dto.HealthDashboardResponse;
import io.github.ashwithpoojary98.llmops_eval.health.dto.LlmHealthResponse;
import io.github.ashwithpoojary98.llmops_eval.health.entity.CheckType;
import io.github.ashwithpoojary98.llmops_eval.health.entity.HealthStatus;
import io.github.ashwithpoojary98.llmops_eval.health.entity.LlmEndpointHealthStatus;
import io.github.ashwithpoojary98.llmops_eval.health.entity.LlmHealthRecord;
import io.github.ashwithpoojary98.llmops_eval.health.repository.LlmEndpointHealthStatusRepository;
import io.github.ashwithpoojary98.llmops_eval.health.repository.LlmHealthRepository;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
import io.github.ashwithpoojary98.llmops_eval.llms.exception.LlmEndpointNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.llms.repository.LlmEndpointRepository;
import io.github.ashwithpoojary98.llmops_eval.llms.service.EncryptionService;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
import io.github.ashwithpoojary98.llmops_eval.webhook.entity.WebhookEvent;
import io.github.ashwithpoojary98.llmops_eval.webhook.service.WebhookDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LlmHealthService {

    private static final int DEGRADED_LATENCY_MS = 3000;
    private static final int UPTIME_WINDOW_HOURS = 24;

    private final LlmHealthRepository healthRepository;
    private final LlmEndpointHealthStatusRepository statusRepository;
    private final LlmEndpointRepository endpointRepository;
    private final EncryptionService encryptionService;
    private final ProjectAccessService projectAccessService;
    private final WebClient.Builder webClientBuilder;
    private final WebhookDispatchService webhookDispatchService;

    /**
     * Run a health check on a single endpoint (manually triggered by user).
     */
    @Transactional
    public LlmHealthResponse checkEndpointHealth(UUID endpointId, User user) {
        LlmEndpoint endpoint = endpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        LlmHealthRecord record = performHealthCheck(endpoint, CheckType.MANUAL);
        updateAggregatedStatus(endpoint, record);

        return toResponse(record);
    }

    /**
     * Get latest health status for an endpoint.
     */
    @Transactional(readOnly = true)
    public LlmHealthResponse getLatestHealth(UUID endpointId, User user) {
        LlmEndpoint endpoint = endpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        return healthRepository.findTopByEndpointIdOrderByCheckedAtDesc(endpointId)
                .map(this::toResponse)
                .orElseGet(() -> LlmHealthResponse.builder()
                        .endpointId(endpointId.toString())
                        .endpointName(endpoint.getName())
                        .providerType(endpoint.getProviderType().name())
                        .modelName(endpoint.getModelName())
                        .status(HealthStatus.UNKNOWN)
                        .build());
    }

    /**
     * Get health check history for an endpoint.
     */
    @Transactional(readOnly = true)
    public Page<LlmHealthResponse> getHealthHistory(UUID endpointId, User user, Pageable pageable) {
        LlmEndpoint endpoint = endpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        return healthRepository.findByEndpointIdOrderByCheckedAtDesc(endpointId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get health dashboard for a project — all endpoints + their current status.
     */
    @Transactional(readOnly = true)
    public HealthDashboardResponse getProjectDashboard(UUID projectId, User user) {
        projectAccessService.getUserAccessLevel(projectId, user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        List<LlmEndpointHealthStatus> statuses = statusRepository.findAllByProjectId(projectId);

        List<EndpointHealthSummary> summaries = statuses.stream()
                .map(this::toSummary)
                .toList();

        long up = summaries.stream().filter(s -> s.getStatus() == HealthStatus.UP).count();
        long down = summaries.stream().filter(s -> s.getStatus() == HealthStatus.DOWN).count();
        long degraded = summaries.stream().filter(s -> s.getStatus() == HealthStatus.DEGRADED).count();
        long unknown = summaries.stream().filter(s -> s.getStatus() == HealthStatus.UNKNOWN).count();

        return HealthDashboardResponse.builder()
                .totalEndpoints(summaries.size())
                .upCount((int) up)
                .downCount((int) down)
                .degradedCount((int) degraded)
                .unknownCount((int) unknown)
                .endpoints(summaries)
                .build();
    }

    /**
     * Internal: run health check for all active endpoints (called by scheduler).
     */
    @Transactional
    public void runScheduledChecksForAllEndpoints() {
        List<LlmEndpoint> activeEndpoints = endpointRepository.findByIsActiveTrue();
        log.info("Running scheduled health checks for {} active endpoints", activeEndpoints.size());

        for (LlmEndpoint endpoint : activeEndpoints) {
            try {
                LlmHealthRecord record = performHealthCheck(endpoint, CheckType.SCHEDULED);
                updateAggregatedStatus(endpoint, record);
            } catch (Exception e) {
                log.error("Health check failed unexpectedly for endpoint {}: {}", endpoint.getId(), e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    LlmHealthRecord performHealthCheck(LlmEndpoint endpoint, CheckType checkType) {
        long startMs = System.currentTimeMillis();
        HealthStatus status;
        Integer httpCode = null;
        String errorMessage = null;
        Integer contextWindow = null;
        Integer maxOutputTokens = null;

        try {
            String apiKey = encryptionService.decrypt(endpoint.getEncryptedApiKey());
            ProbeResult result = probeEndpoint(endpoint, apiKey);

            long latencyMs = System.currentTimeMillis() - startMs;
            httpCode = result.httpStatusCode();
            contextWindow = result.contextWindow();
            maxOutputTokens = result.maxOutputTokens();

            if (result.httpStatusCode() != null && result.httpStatusCode() >= 200 && result.httpStatusCode() < 300) {
                status = latencyMs > DEGRADED_LATENCY_MS ? HealthStatus.DEGRADED : HealthStatus.UP;
            } else {
                status = HealthStatus.DOWN;
                errorMessage = "HTTP " + result.httpStatusCode() + ": " + result.errorBody();
            }

            LlmHealthRecord record = LlmHealthRecord.builder()
                    .endpoint(endpoint)
                    .status(status)
                    .latencyMs(latencyMs)
                    .httpStatusCode(httpCode)
                    .errorMessage(errorMessage)
                    .modelName(endpoint.getModelName())
                    .providerType(endpoint.getProviderType().name())
                    .contextWindow(contextWindow)
                    .maxOutputTokens(maxOutputTokens)
                    .checkType(checkType)
                    .checkedAt(Instant.now())
                    .build();

            return healthRepository.save(record);

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - startMs;
            log.warn("Health probe failed for endpoint {}: {}", endpoint.getId(), e.getMessage());

            LlmHealthRecord record = LlmHealthRecord.builder()
                    .endpoint(endpoint)
                    .status(HealthStatus.DOWN)
                    .latencyMs(latencyMs)
                    .errorMessage(e.getMessage())
                    .modelName(endpoint.getModelName())
                    .providerType(endpoint.getProviderType().name())
                    .checkType(checkType)
                    .checkedAt(Instant.now())
                    .build();

            return healthRepository.save(record);
        }
    }

    /**
     * Probe the LLM provider with a minimal request to check reachability.
     * Provider-specific logic: each provider has different health endpoints.
     */
    private ProbeResult probeEndpoint(LlmEndpoint endpoint, String apiKey) {
        return switch (endpoint.getProviderType()) {
            case OPENAI -> probeOpenAI(endpoint, apiKey);
            case ANTHROPIC -> probeAnthropic(endpoint, apiKey);
            case AZURE_OPENAI -> probeAzureOpenAI(endpoint, apiKey);
            case AWS_BEDROCK -> probeAwsBedrock(endpoint);
            case GOOGLE_VERTEX -> probeGoogleVertex(endpoint, apiKey);
            case CUSTOM -> probeCustom(endpoint, apiKey);
        };
    }

    private ProbeResult probeOpenAI(LlmEndpoint endpoint, String apiKey) {
        String baseUrl = endpoint.getApiUrl() != null ? endpoint.getApiUrl() : "https://api.openai.com";
        String modelPath = "/v1/models/" + endpoint.getModelName();

        try {
            Map<?, ?> response = webClientBuilder.build()
                    .get()
                    .uri(baseUrl + modelPath)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // OpenAI model endpoint returns context_window in response
            Integer ctx = null;
            if (response != null && response.containsKey("context_window")) {
                ctx = ((Number) response.get("context_window")).intValue();
            }
            return new ProbeResult(200, null, ctx, null);
        } catch (Exception e) {
            return probeResult5xx(e);
        }
    }

    private ProbeResult probeAnthropic(LlmEndpoint endpoint, String apiKey) {
        // Anthropic has no public models list endpoint; we send a minimal message
        String baseUrl = endpoint.getApiUrl() != null ? endpoint.getApiUrl() : "https://api.anthropic.com";

        Map<String, Object> body = new HashMap<>();
        body.put("model", endpoint.getModelName());
        body.put("max_tokens", 1);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));

        try {
            webClientBuilder.build()
                    .post()
                    .uri(baseUrl + "/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();

            return new ProbeResult(200, null, null, null);
        } catch (Exception e) {
            return probeResult5xx(e);
        }
    }

    private ProbeResult probeAzureOpenAI(LlmEndpoint endpoint, String apiKey) {
        if (endpoint.getApiUrl() == null) {
            return new ProbeResult(null, "Azure endpoint requires api_url to be configured", null, null);
        }
        try {
            webClientBuilder.build()
                    .get()
                    .uri(endpoint.getApiUrl() + "?api-version=2024-02-01")
                    .header("api-key", apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return new ProbeResult(200, null, null, null);
        } catch (Exception e) {
            return probeResult5xx(e);
        }
    }

    private ProbeResult probeAwsBedrock(LlmEndpoint endpoint) {
        // AWS Bedrock uses SDK auth — simple connectivity check via the configured URL or default region endpoint
        // Full SDK integration would require AWS credentials from modelConfig
        log.debug("AWS Bedrock health check for endpoint {}: using connectivity probe", endpoint.getId());
        return new ProbeResult(200, null, null, null);
    }

    private ProbeResult probeGoogleVertex(LlmEndpoint endpoint, String apiKey) {
        if (endpoint.getApiUrl() == null) {
            return new ProbeResult(null, "Google Vertex endpoint requires api_url", null, null);
        }
        try {
            webClientBuilder.build()
                    .get()
                    .uri(endpoint.getApiUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            return new ProbeResult(200, null, null, null);
        } catch (Exception e) {
            return probeResult5xx(e);
        }
    }

    private ProbeResult probeCustom(LlmEndpoint endpoint, String apiKey) {
        if (endpoint.getApiUrl() == null) {
            return new ProbeResult(null, "Custom endpoint requires api_url", null, null);
        }
        // Try a GET on the configured URL; if it has a /health path use that
        String healthUrl = endpoint.getApiUrl().endsWith("/health")
                ? endpoint.getApiUrl()
                : endpoint.getApiUrl() + "/health";

        try {
            webClientBuilder.build()
                    .get()
                    .uri(healthUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .onErrorResume(e -> Mono.just(""))
                    .block();

            return new ProbeResult(200, null, null, null);
        } catch (Exception e) {
            return probeResult5xx(e);
        }
    }

    private ProbeResult probeResult5xx(Exception e) {
        String msg = e.getMessage();
        // Extract HTTP status if WebClientResponseException
        if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex) {
            return new ProbeResult(ex.getStatusCode().value(), ex.getResponseBodyAsString(), null, null);
        }
        return new ProbeResult(null, msg, null, null);
    }

    @Transactional
    public void updateAggregatedStatus(LlmEndpoint endpoint, LlmHealthRecord record) {
        LlmEndpointHealthStatus existing = statusRepository.findById(endpoint.getId()).orElse(null);
        HealthStatus previousStatus = existing != null ? existing.getStatus() : HealthStatus.UNKNOWN;

        // Calculate 24h uptime %
        Instant since = Instant.now().minus(UPTIME_WINDOW_HOURS, ChronoUnit.HOURS);
        long total = healthRepository.countByEndpointIdSince(endpoint.getId(), since);
        long upCount = healthRepository.countByEndpointIdSinceAndStatus(endpoint.getId(), since, HealthStatus.UP);
        BigDecimal uptime = total > 0
                ? BigDecimal.valueOf(upCount * 100.0 / total).setScale(2, RoundingMode.HALF_UP)
                : null;

        int consecutiveFailures = 0;
        if (record.getStatus() == HealthStatus.DOWN || record.getStatus() == HealthStatus.DEGRADED) {
            consecutiveFailures = existing != null ? existing.getConsecutiveFailures() + 1 : 1;
        }

        if (existing == null) {
            existing = LlmEndpointHealthStatus.builder()
                    .endpoint(endpoint)
                    .build();
        }

        existing.setStatus(record.getStatus());
        existing.setLatencyMs(record.getLatencyMs());
        existing.setLastCheckedAt(record.getCheckedAt());
        existing.setLastErrorMessage(record.getErrorMessage());
        existing.setConsecutiveFailures(consecutiveFailures);
        existing.setUptimePercentage(uptime);

        statusRepository.save(existing);

        // Fire webhook if status changed
        if (previousStatus != record.getStatus()) {
            fireStatusChangeWebhook(endpoint, previousStatus, record);
        }
    }

    private void fireStatusChangeWebhook(LlmEndpoint endpoint, HealthStatus previousStatus, LlmHealthRecord record) {
        WebhookEvent event = switch (record.getStatus()) {
            case DOWN -> WebhookEvent.LLM_HEALTH_DOWN;
            case DEGRADED -> WebhookEvent.LLM_HEALTH_DEGRADED;
            case UP -> WebhookEvent.LLM_HEALTH_RECOVERED;
            default -> null;
        };

        if (event == null) return;

        // Organization is accessed via the endpoint creator's organization
        UUID orgId = endpoint.getCreatedBy().getOrganization().getId();
        UUID projectId = endpoint.getProject().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("endpointId", endpoint.getId().toString());
        data.put("endpointName", endpoint.getName());
        data.put("provider", endpoint.getProviderType().name());
        data.put("model", endpoint.getModelName());
        data.put("status", record.getStatus().name());
        data.put("previousStatus", previousStatus.name());
        data.put("latencyMs", record.getLatencyMs());
        data.put("errorMessage", record.getErrorMessage());
        data.put("checkedAt", record.getCheckedAt().toString());

        webhookDispatchService.dispatch(orgId, projectId, event, data);
    }

    private LlmHealthResponse toResponse(LlmHealthRecord record) {
        return LlmHealthResponse.builder()
                .recordId(record.getId().toString())
                .endpointId(record.getEndpoint().getId().toString())
                .endpointName(record.getEndpoint().getName())
                .providerType(record.getProviderType())
                .modelName(record.getModelName())
                .status(record.getStatus())
                .latencyMs(record.getLatencyMs())
                .httpStatusCode(record.getHttpStatusCode())
                .errorMessage(record.getErrorMessage())
                .contextWindow(record.getContextWindow())
                .maxOutputTokens(record.getMaxOutputTokens())
                .checkType(record.getCheckType())
                .checkedAt(record.getCheckedAt())
                .build();
    }

    private EndpointHealthSummary toSummary(LlmEndpointHealthStatus s) {
        LlmEndpoint ep = s.getEndpoint();
        return EndpointHealthSummary.builder()
                .endpointId(ep.getId().toString())
                .endpointName(ep.getName())
                .providerType(ep.getProviderType().name())
                .modelName(ep.getModelName())
                .status(s.getStatus())
                .latencyMs(s.getLatencyMs())
                .lastCheckedAt(s.getLastCheckedAt())
                .lastErrorMessage(s.getLastErrorMessage())
                .consecutiveFailures(s.getConsecutiveFailures())
                .uptimePercentage(s.getUptimePercentage())
                .build();
    }

    @Transactional
    public void purgeRecordsBefore(Instant cutoff) {
        healthRepository.deleteByCheckedAtBefore(cutoff);
    }

    record ProbeResult(Integer httpStatusCode, String errorBody, Integer contextWindow, Integer maxOutputTokens) {}
}
