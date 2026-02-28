package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "evaluation_scores")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationScore {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_result_id", nullable = false)
    private EvaluationResult evaluationResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private EvaluationMetric metric;

    @Column(nullable = false)
    private Double score; // Normalized 0-1

    @Column(name = "raw_score", nullable = false)
    private Double rawScore;

    @Column
    private Boolean passed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_details", columnDefinition = "jsonb")
    private Map<String, Object> scoreDetails;

    @Column(name = "judge_reasoning", columnDefinition = "TEXT")
    private String judgeReasoning;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
