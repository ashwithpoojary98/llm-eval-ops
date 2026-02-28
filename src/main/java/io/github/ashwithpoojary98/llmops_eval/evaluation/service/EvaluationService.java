package io.github.ashwithpoojary98.llmops_eval.evaluation.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.dataset.entity.Dataset;
import io.github.ashwithpoojary98.llmops_eval.dataset.repository.DatasetRepository;
import io.github.ashwithpoojary98.llmops_eval.evaluation.dto.*;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.*;
import io.github.ashwithpoojary98.llmops_eval.evaluation.exception.EvaluationNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.evaluation.exception.EvaluationValidationException;
import io.github.ashwithpoojary98.llmops_eval.evaluation.repository.*;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.llms.exception.LlmEndpointNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.llms.repository.LlmEndpointRepository;
import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRunRepository evaluationRunRepository;
    private final EvaluationMetricRepository evaluationMetricRepository;
    private final EvaluationResultRepository evaluationResultRepository;
    private final EvaluationRunMetricRepository evaluationRunMetricRepository;
    private final ProjectRepository projectRepository;
    private final DatasetRepository datasetRepository;
    private final LlmEndpointRepository llmEndpointRepository;
    private final ProjectAccessService projectAccessService;
    private final EvaluationEngineClient evaluationEngineClient;

    /**
     * Start a new evaluation run.
     */
    @Transactional
    public EvaluationRunResponse startEvaluation(UUID projectId, StartEvaluationRequest request, User user) {
        log.info("Starting evaluation '{}' for project {}", request.getName(), projectId);

        // Validate project access
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(project.getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.WRITE) {
            throw new ForbiddenException("You don't have permission to run evaluations in this project");
        }

        // Validate dataset
        Dataset dataset = datasetRepository.findByIdWithTestCases(request.getDatasetId())
                .orElseThrow(() -> new EvaluationValidationException("Dataset not found: " + request.getDatasetId()));

        if (!dataset.getProject().getId().equals(projectId)) {
            throw new EvaluationValidationException("Dataset does not belong to this project");
        }

        if (dataset.getTestCases().isEmpty()) {
            throw new EvaluationValidationException("Dataset has no test cases");
        }

        // Validate LLM endpoints
        LlmEndpoint targetEndpoint = null;
        LlmEndpoint judgeEndpoint = null;

        if (request.getEvaluationMode() == EvaluationMode.LIVE_GENERATION) {
            if (request.getTargetLlmEndpointId() == null) {
                throw new EvaluationValidationException("Target LLM endpoint is required for LIVE_GENERATION mode");
            }
            targetEndpoint = llmEndpointRepository.findById(request.getTargetLlmEndpointId())
                    .orElseThrow(() -> new LlmEndpointNotFoundException("Target LLM endpoint not found"));
        }

        // Validate metrics
        List<EvaluationMetric> metrics = new ArrayList<>();
        boolean requiresJudge = false;

        for (MetricConfigRequest metricConfig : request.getMetrics()) {
            EvaluationMetric metric = evaluationMetricRepository.findById(metricConfig.getMetricId())
                    .orElseThrow(() -> new EvaluationValidationException("Metric not found: " + metricConfig.getMetricId()));

            metrics.add(metric);

            if (Boolean.TRUE.equals(metric.getRequiresJudgeLlm())) {
                requiresJudge = true;
            }
        }

        // Validate judge endpoint if needed
        if (requiresJudge) {
            if (request.getJudgeLlmEndpointId() == null) {
                throw new EvaluationValidationException("Judge LLM endpoint is required for selected metrics");
            }
            judgeEndpoint = llmEndpointRepository.findById(request.getJudgeLlmEndpointId())
                    .orElseThrow(() -> new LlmEndpointNotFoundException("Judge LLM endpoint not found"));
        }

        // Get next run number
        Integer runNumber = evaluationRunRepository.getNextRunNumber(projectId, request.getName());

        // Create evaluation run
        EvaluationRun run = EvaluationRun.builder()
                .project(project)
                .dataset(dataset)
                .name(request.getName())
                .runNumber(runNumber)
                .evaluationMode(request.getEvaluationMode())
                .targetLlmEndpoint(targetEndpoint)
                .judgeLlmEndpoint(judgeEndpoint)
                .status(EvaluationStatus.PENDING)
                .totalTestCases(dataset.getTestCases().size())
                .triggerType(request.getTriggerType())
                .triggerReference(request.getTriggerReference())
                .createdBy(user)
                .build();

        run = evaluationRunRepository.save(run);

        // Create run metrics configuration
        for (MetricConfigRequest metricConfig : request.getMetrics()) {
            EvaluationMetric metric = metrics.stream()
                    .filter(m -> m.getId().equals(metricConfig.getMetricId()))
                    .findFirst()
                    .orElseThrow();

            EvaluationRunMetric runMetric = EvaluationRunMetric.builder()
                    .evaluationRun(run)
                    .metric(metric)
                    .weight(metricConfig.getWeight() != null ? metricConfig.getWeight() : metric.getDefaultWeight())
                    .passThreshold(metricConfig.getPassThreshold() != null ? metricConfig.getPassThreshold() : metric.getDefaultThreshold())
                    .customPromptTemplate(metricConfig.getCustomPromptTemplate())
                    .build();

            evaluationRunMetricRepository.save(runMetric);
        }

        // Send to evaluation engine asynchronously
        evaluationEngineClient.submitEvaluation(run, dataset, metrics, request.getMetrics());

        log.info("Evaluation run {} created and submitted to evaluation engine", run.getId());

        return toEvaluationRunResponse(run, false);
    }

    /**
     * Get evaluation run by ID.
     */
    @Transactional(readOnly = true)
    public EvaluationRunResponse getEvaluationRun(UUID runId, User user) {
        EvaluationRun run = evaluationRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        // Check access
        projectAccessService.getUserAccessLevel(run.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        return toEvaluationRunResponse(run, true);
    }

    /**
     * List evaluation runs for a project.
     */
    @Transactional(readOnly = true)
    public Page<EvaluationRunSummaryResponse> listEvaluationRuns(
            UUID projectId, EvaluationStatus status, String search, User user, Pageable pageable) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        projectAccessService.getUserAccessLevel(project.getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        Page<EvaluationRun> runs;
        if (search != null && !search.isBlank()) {
            runs = evaluationRunRepository.searchByProjectId(projectId, search, pageable);
        } else if (status != null) {
            runs = evaluationRunRepository.findByProjectIdAndStatus(projectId, status, pageable);
        } else {
            runs = evaluationRunRepository.findByProjectId(projectId, pageable);
        }

        return runs.map(this::toEvaluationRunSummaryResponse);
    }

    /**
     * Get evaluation results for a run.
     */
    @Transactional(readOnly = true)
    public Page<EvaluationResultResponse> getEvaluationResults(UUID runId, User user, Pageable pageable) {
        EvaluationRun run = evaluationRunRepository.findById(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        projectAccessService.getUserAccessLevel(run.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        Page<EvaluationResult> results = evaluationResultRepository.findByEvaluationRunId(runId, pageable);

        return results.map(this::toEvaluationResultResponse);
    }

    /**
     * Cancel a running evaluation.
     */
    @Transactional
    public void cancelEvaluation(UUID runId, User user) {
        EvaluationRun run = evaluationRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(run.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.WRITE) {
            throw new ForbiddenException("You don't have permission to cancel this evaluation");
        }

        if (run.getStatus() != EvaluationStatus.PENDING && run.getStatus() != EvaluationStatus.RUNNING) {
            throw new EvaluationValidationException("Cannot cancel evaluation with status: " + run.getStatus());
        }

        run.setStatus(EvaluationStatus.CANCELLED);
        run.setCompletedAt(Instant.now());
        evaluationRunRepository.save(run);

        // Notify evaluation engine
        evaluationEngineClient.cancelEvaluation(runId.toString());

        log.info("Evaluation run {} cancelled", runId);
    }

    /**
     * Delete an evaluation run permanently.
     * Cannot delete if evaluation is still running or pending.
     */
    @Transactional
    public void deleteEvaluation(UUID runId, User user) {
        EvaluationRun run = evaluationRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(run.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN) {
            throw new ForbiddenException("Only admins can delete evaluation runs");
        }

        // Prevent deletion if evaluation is still running or pending
        if (run.getStatus() == EvaluationStatus.PENDING || run.getStatus() == EvaluationStatus.RUNNING) {
            throw new EvaluationValidationException(
                    "Cannot delete evaluation while it is " + run.getStatus() +
                    ". Please cancel it first or wait for completion.");
        }

        // Delete results and scores first (cascade should handle this, but being explicit)
        evaluationResultRepository.deleteByEvaluationRunId(runId);
        evaluationRunMetricRepository.deleteByEvaluationRunId(runId);
        evaluationRunRepository.delete(run);

        log.info("Evaluation run {} deleted by user {}", runId, user.getEmail());
    }

    /**
     * Retrigger a failed or completed evaluation with the same configuration.
     */
    @Transactional
    public EvaluationRunResponse retriggerEvaluation(UUID runId, User user) {
        EvaluationRun originalRun = evaluationRunRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(originalRun.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.WRITE) {
            throw new ForbiddenException("You don't have permission to run evaluations in this project");
        }

        // Get original metrics configuration
        List<EvaluationRunMetric> originalMetrics = evaluationRunMetricRepository
                .findByEvaluationRunIdWithMetric(originalRun.getId());

        // Create new StartEvaluationRequest from original run
        List<MetricConfigRequest> metricConfigs = originalMetrics.stream()
                .map(rm -> {
                    MetricConfigRequest config = new MetricConfigRequest();
                    config.setMetricId(rm.getMetric().getId());
                    config.setWeight(rm.getWeight());
                    config.setPassThreshold(rm.getPassThreshold());
                    config.setCustomPromptTemplate(rm.getCustomPromptTemplate());
                    return config;
                })
                .collect(Collectors.toList());

        StartEvaluationRequest request = new StartEvaluationRequest();
        request.setName(originalRun.getName());
        request.setDatasetId(originalRun.getDataset().getId());
        request.setEvaluationMode(originalRun.getEvaluationMode());
        request.setTargetLlmEndpointId(originalRun.getTargetLlmEndpoint() != null ?
                originalRun.getTargetLlmEndpoint().getId() : null);
        request.setJudgeLlmEndpointId(originalRun.getJudgeLlmEndpoint() != null ?
                originalRun.getJudgeLlmEndpoint().getId() : null);
        request.setMetrics(metricConfigs);
        request.setTriggerType(TriggerType.MANUAL);
        request.setTriggerReference("Retrigger of run #" + originalRun.getRunNumber());

        log.info("Retriggering evaluation run {} as new run", runId);

        return startEvaluation(originalRun.getProject().getId(), request, user);
    }

    /**
     * Get available metrics for a project.
     */
    @Transactional(readOnly = true)
    public List<EvaluationMetricResponse> getAvailableMetrics(UUID projectId, User user) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        List<EvaluationMetric> metrics = evaluationMetricRepository.findAvailableMetricsForProject(projectId);

        return metrics.stream()
                .map(this::toEvaluationMetricResponse)
                .collect(Collectors.toList());
    }

    /**
     * Handle callback from evaluation engine when evaluation completes.
     */
    @Transactional
    public void handleEvaluationCallback(String evaluationRunId, EvaluationCallbackPayload payload) {
        log.info("Received callback for evaluation run: {}, status: {}", evaluationRunId, payload.getStatus());

        UUID runId = UUID.fromString(evaluationRunId);
        EvaluationRun run = evaluationRunRepository.findById(runId)
                .orElseThrow(() -> new EvaluationNotFoundException("Evaluation run not found: " + runId));

        // Update status
        run.setStatus(EvaluationStatus.valueOf(payload.getStatus()));
        run.setOverallScore(payload.getOverallScore());
        run.setCompletedTestCases(payload.getCompleted());
        run.setFailedTestCases(payload.getFailed());
        run.setCompletedAt(Instant.now());

        if (payload.getError() != null) {
            run.setErrorMessage(payload.getError());
        }

        evaluationRunRepository.save(run);

        log.info("Evaluation run {} updated: status={}, score={}",
                runId, payload.getStatus(), payload.getOverallScore());
    }

    // Mapping methods

    private EvaluationRunResponse toEvaluationRunResponse(EvaluationRun run, boolean includeMetrics) {
        EvaluationRunResponse.EvaluationRunResponseBuilder builder = EvaluationRunResponse.builder()
                .id(run.getId().toString())
                .projectId(run.getProject().getId().toString())
                .projectName(run.getProject().getName())
                .datasetId(run.getDataset().getId().toString())
                .datasetName(run.getDataset().getName())
                .name(run.getName())
                .runNumber(run.getRunNumber())
                .evaluationMode(run.getEvaluationMode())
                .status(run.getStatus())
                .totalTestCases(run.getTotalTestCases())
                .completedTestCases(run.getCompletedTestCases())
                .failedTestCases(run.getFailedTestCases())
                .overallScore(run.getOverallScore())
                .totalTokens(run.getTotalTokens())
                .totalCost(run.getTotalCost())
                .triggerType(run.getTriggerType())
                .triggerReference(run.getTriggerReference())
                .errorMessage(run.getErrorMessage())
                .createdById(run.getCreatedBy().getId().toString())
                .createdByName(run.getCreatedBy().getFullName())
                .createdAt(run.getCreatedAt())
                .startedAt(run.getStartedAt())
                .completedAt(run.getCompletedAt());

        if (run.getTargetLlmEndpoint() != null) {
            builder.targetLlmEndpointId(run.getTargetLlmEndpoint().getId().toString())
                   .targetLlmEndpointName(run.getTargetLlmEndpoint().getName());
        }

        if (run.getJudgeLlmEndpoint() != null) {
            builder.judgeLlmEndpointId(run.getJudgeLlmEndpoint().getId().toString())
                   .judgeLlmEndpointName(run.getJudgeLlmEndpoint().getName());
        }

        if (includeMetrics) {
            List<EvaluationRunMetric> runMetrics = evaluationRunMetricRepository.findByEvaluationRunIdWithMetric(run.getId());
            List<EvaluationRunMetricResponse> metricResponses = runMetrics.stream()
                    .map(rm -> EvaluationRunMetricResponse.builder()
                            .metricId(rm.getMetric().getId().toString())
                            .metricCode(rm.getMetric().getCode())
                            .metricName(rm.getMetric().getDisplayName())
                            .category(rm.getMetric().getCategory())
                            .weight(rm.getWeight())
                            .passThreshold(rm.getPassThreshold())
                            .hasCustomPrompt(rm.getCustomPromptTemplate() != null)
                            .build())
                    .collect(Collectors.toList());
            builder.metrics(metricResponses);
        }

        return builder.build();
    }

    private EvaluationRunSummaryResponse toEvaluationRunSummaryResponse(EvaluationRun run) {
        return EvaluationRunSummaryResponse.builder()
                .id(run.getId().toString())
                .name(run.getName())
                .runNumber(run.getRunNumber())
                .datasetName(run.getDataset() != null ? run.getDataset().getName() : null)
                .status(run.getStatus())
                .totalTestCases(run.getTotalTestCases())
                .completedTestCases(run.getCompletedTestCases())
                .overallScore(run.getOverallScore())
                .createdAt(run.getCreatedAt())
                .completedAt(run.getCompletedAt())
                .build();
    }

    private EvaluationResultResponse toEvaluationResultResponse(EvaluationResult result) {
        return EvaluationResultResponse.builder()
                .id(result.getId().toString())
                .testCaseId(result.getTestCase().getId().toString())
                .question(result.getTestCase().getQuestion())
                .groundTruth(result.getTestCase().getGroundTruth())
                .llmOutput(result.getTestCase().getLlmOutput())
                .generatedOutput(result.getGeneratedOutput())
                .status(result.getStatus())
                .llmLatencyMs(result.getLlmLatencyMs())
                .processingTimeMs(result.getProcessingTimeMs())
                .inputTokens(result.getInputTokens())
                .outputTokens(result.getOutputTokens())
                .errorMessage(result.getErrorMessage())
                .createdAt(result.getCreatedAt())
                .scores(result.getScores().stream()
                        .map(s -> EvaluationScoreResponse.builder()
                                .metricCode(s.getMetric().getCode())
                                .metricName(s.getMetric().getDisplayName())
                                .score(s.getScore())
                                .rawScore(s.getRawScore())
                                .passed(s.getPassed())
                                .details(s.getScoreDetails())
                                .judgeReasoning(s.getJudgeReasoning())
                                .errorMessage(s.getErrorMessage())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    private EvaluationMetricResponse toEvaluationMetricResponse(EvaluationMetric metric) {
        return EvaluationMetricResponse.builder()
                .id(metric.getId().toString())
                .code(metric.getCode())
                .displayName(metric.getDisplayName())
                .description(metric.getDescription())
                .category(metric.getCategory())
                .requiresGroundTruth(metric.getRequiresGroundTruth())
                .requiresContext(metric.getRequiresContext())
                .requiresJudgeLlm(metric.getRequiresJudgeLlm())
                .defaultWeight(metric.getDefaultWeight())
                .defaultThreshold(metric.getDefaultThreshold())
                .isSystem(metric.getIsSystem())
                .isActive(metric.getIsActive())
                .createdAt(metric.getCreatedAt())
                .build();
    }
}
