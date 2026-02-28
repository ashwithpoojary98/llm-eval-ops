package io.github.ashwithpoojary98.llmops_eval.evaluation.service;

import io.github.ashwithpoojary98.llmops_eval.dataset.entity.Dataset;
import io.github.ashwithpoojary98.llmops_eval.dataset.entity.DatasetTestCase;
import io.github.ashwithpoojary98.llmops_eval.evaluation.dto.MetricConfigRequest;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationMetric;
import io.github.ashwithpoojary98.llmops_eval.evaluation.entity.EvaluationRun;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.llms.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Client for communicating with the FastAPI evaluation engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EvaluationEngineClient {

    private final EncryptionService encryptionService;
    private final WebClient.Builder webClientBuilder;

    @Value("${evaluation.engine.url:http://localhost:8000}")
    private String evaluationEngineUrl;

    @Value("${evaluation.engine.callback-url:http://localhost:8080/api/evaluations/callback}")
    private String callbackUrl;

    /**
     * Submit an evaluation to the FastAPI engine.
     */
    @Async
    public void submitEvaluation(
            EvaluationRun run,
            Dataset dataset,
            List<EvaluationMetric> metrics,
            List<MetricConfigRequest> metricConfigs) {

        log.info("Submitting evaluation {} to engine at {}", run.getId(), evaluationEngineUrl);

        try {
            Map<String, Object> payload = buildEvaluationPayload(run, dataset, metrics, metricConfigs);

            WebClient client = webClientBuilder
                    .baseUrl(evaluationEngineUrl)
                    .build();

            client.post()
                    .uri("/evaluate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .subscribe(
                            response -> log.info("Evaluation submitted successfully: {}", response),
                            error -> log.error("Failed to submit evaluation: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("Error submitting evaluation to engine", e);
        }
    }

    /**
     * Cancel a running evaluation.
     */
    public void cancelEvaluation(String evaluationRunId) {
        log.info("Cancelling evaluation {} in engine", evaluationRunId);

        try {
            WebClient client = webClientBuilder
                    .baseUrl(evaluationEngineUrl)
                    .build();

            client.delete()
                    .uri("/evaluate/{id}", evaluationRunId)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .subscribe(
                            response -> log.info("Evaluation cancellation sent"),
                            error -> log.warn("Failed to cancel evaluation in engine: {}", error.getMessage())
                    );

        } catch (Exception e) {
            log.error("Error cancelling evaluation in engine", e);
        }
    }

    /**
     * Build the payload for the FastAPI evaluation engine.
     */
    private Map<String, Object> buildEvaluationPayload(
            EvaluationRun run,
            Dataset dataset,
            List<EvaluationMetric> metrics,
            List<MetricConfigRequest> metricConfigs) {

        Map<String, Object> payload = new HashMap<>();

        payload.put("evaluation_run_id", run.getId().toString());
        payload.put("project_id", run.getProject().getId().toString());
        payload.put("evaluation_mode", run.getEvaluationMode().name());

        // Build test cases
        List<Map<String, Object>> testCases = dataset.getTestCases().stream()
                .map(dtc -> buildTestCasePayload(dtc))
                .collect(Collectors.toList());
        payload.put("test_cases", testCases);

        // Build metrics configuration
        List<Map<String, Object>> metricsPayload = new ArrayList<>();
        for (int i = 0; i < metrics.size(); i++) {
            EvaluationMetric metric = metrics.get(i);
            MetricConfigRequest config = metricConfigs.get(i);

            Map<String, Object> metricPayload = new HashMap<>();
            metricPayload.put("id", metric.getId().toString());
            metricPayload.put("code", metric.getCode());
            metricPayload.put("weight", config.getWeight() != null ? config.getWeight() : metric.getDefaultWeight());
            metricPayload.put("pass_threshold", config.getPassThreshold() != null ?
                    config.getPassThreshold() : metric.getDefaultThreshold());
            metricPayload.put("judge_prompt_template", config.getCustomPromptTemplate() != null ?
                    config.getCustomPromptTemplate() : metric.getJudgePromptTemplate());

            metricsPayload.add(metricPayload);
        }
        payload.put("metrics", metricsPayload);

        // Add target LLM endpoint if LIVE_GENERATION
        if (run.getTargetLlmEndpoint() != null) {
            payload.put("target_llm_endpoint", buildLlmEndpointPayload(run.getTargetLlmEndpoint()));
        }

        // Add judge LLM endpoint
        if (run.getJudgeLlmEndpoint() != null) {
            payload.put("judge_llm_endpoint", buildLlmEndpointPayload(run.getJudgeLlmEndpoint()));
        }

        // Add callback URL
        payload.put("callback_url", callbackUrl + "/" + run.getId());

        return payload;
    }

    private Map<String, Object> buildTestCasePayload(DatasetTestCase dtc) {
        Map<String, Object> testCase = new HashMap<>();
        testCase.put("id", dtc.getTestCase().getId().toString());
        testCase.put("question", dtc.getTestCase().getQuestion());

        if (dtc.getTestCase().getLlmOutput() != null) {
            testCase.put("llm_output", dtc.getTestCase().getLlmOutput());
        }
        if (dtc.getTestCase().getGroundTruth() != null) {
            testCase.put("ground_truth", dtc.getTestCase().getGroundTruth());
        }
        if (dtc.getTestCase().getRetrievedContext() != null && !dtc.getTestCase().getRetrievedContext().isEmpty()) {
            testCase.put("retrieved_context", dtc.getTestCase().getRetrievedContext());
        }

        return testCase;
    }

    private Map<String, Object> buildLlmEndpointPayload(LlmEndpoint endpoint) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("provider_type", endpoint.getProviderType().name());
        payload.put("model_name", endpoint.getModelName());
        payload.put("api_key", encryptionService.decrypt(endpoint.getEncryptedApiKey()));

        if (endpoint.getApiUrl() != null) {
            payload.put("api_url", endpoint.getApiUrl());
        }
        if (endpoint.getModelConfig() != null) {
            payload.put("config", endpoint.getModelConfig());
        }

        return payload;
    }
}
