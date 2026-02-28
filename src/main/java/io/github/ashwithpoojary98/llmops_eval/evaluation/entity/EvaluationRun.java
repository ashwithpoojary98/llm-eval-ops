package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.dataset.entity.Dataset;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "evaluation_runs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "run_number", nullable = false)
    @Builder.Default
    private Integer runNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 20)
    @Builder.Default
    private EvaluationMode evaluationMode = EvaluationMode.PRE_GENERATED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_llm_endpoint_id")
    private LlmEndpoint targetLlmEndpoint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "judge_llm_endpoint_id")
    private LlmEndpoint judgeLlmEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EvaluationStatus status = EvaluationStatus.PENDING;

    @Column(name = "total_test_cases", nullable = false)
    @Builder.Default
    private Integer totalTestCases = 0;

    @Column(name = "completed_test_cases", nullable = false)
    @Builder.Default
    private Integer completedTestCases = 0;

    @Column(name = "failed_test_cases", nullable = false)
    @Builder.Default
    private Integer failedTestCases = 0;

    @Column(name = "overall_score")
    private Double overallScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metric_summaries", columnDefinition = "jsonb")
    private Map<String, Object> metricSummaries;

    @Column(name = "total_tokens")
    @Builder.Default
    private Long totalTokens = 0L;

    @Column(name = "total_cost", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    @Builder.Default
    private TriggerType triggerType = TriggerType.MANUAL;

    @Column(name = "trigger_reference", length = 255)
    private String triggerReference;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // Relationships
    @OneToMany(mappedBy = "evaluationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationResult> results = new ArrayList<>();

    @OneToMany(mappedBy = "evaluationRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationRunMetric> runMetrics = new ArrayList<>();
}
