package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "evaluation_run_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRunMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluation_run_id", nullable = false)
    private EvaluationRun evaluationRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private EvaluationMetric metric;

    @Column(nullable = false)
    @Builder.Default
    private Double weight = 1.0;

    @Column(name = "pass_threshold")
    private Double passThreshold;

    @Column(name = "custom_prompt_template", columnDefinition = "TEXT")
    private String customPromptTemplate;
}
