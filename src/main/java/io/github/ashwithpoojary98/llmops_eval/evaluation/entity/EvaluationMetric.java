package io.github.ashwithpoojary98.llmops_eval.evaluation.entity;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evaluation_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project; // NULL means system/global metric

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MetricCategory category;

    @Column(name = "requires_ground_truth", nullable = false)
    @Builder.Default
    private Boolean requiresGroundTruth = false;

    @Column(name = "requires_context", nullable = false)
    @Builder.Default
    private Boolean requiresContext = false;

    @Column(name = "requires_judge_llm", nullable = false)
    @Builder.Default
    private Boolean requiresJudgeLlm = false;

    @Column(name = "judge_prompt_template", columnDefinition = "TEXT")
    private String judgePromptTemplate;

    @Column(name = "default_weight", nullable = false)
    @Builder.Default
    private Double defaultWeight = 1.0;

    @Column(name = "default_threshold")
    private Double defaultThreshold;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
