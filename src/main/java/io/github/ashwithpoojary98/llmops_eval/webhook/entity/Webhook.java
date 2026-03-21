package io.github.ashwithpoojary98.llmops_eval.webhook.entity;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "webhooks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Webhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "secret_encrypted", columnDefinition = "TEXT")
    private String secretEncrypted;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> events = List.of();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Integer failureCount = 0;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
