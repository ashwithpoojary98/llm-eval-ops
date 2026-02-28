package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

import io.github.ashwithpoojary98.llmops_eval.testcases.entity.TestCase;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evaluation_results")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_run_id", nullable = false)
    private EvaluationRun evaluationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_case_id", nullable = false)
    private TestCase testCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ResultStatus status = ResultStatus.PENDING;

    @Column(name = "generated_output", columnDefinition = "TEXT")
    private String generatedOutput;

    @Column(name = "llm_latency_ms")
    private Integer llmLatencyMs;

    @Column(name = "judge_latency_ms")
    private Integer judgeLatencyMs;

    @Column(name = "processing_time_ms")
    private Integer processingTimeMs;

    @Column(name = "input_tokens", nullable = false)
    @Builder.Default
    private Integer inputTokens = 0;

    @Column(name = "output_tokens", nullable = false)
    @Builder.Default
    private Integer outputTokens = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "evaluationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EvaluationScore> scores = new ArrayList<>();
}
