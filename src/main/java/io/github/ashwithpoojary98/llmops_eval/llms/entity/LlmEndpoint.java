package io.github.ashwithpoojary98.llmops_eval.llms.entity;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "llm_endpoints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "api_url", length = 500)
    private String apiUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    @Builder.Default
    private AuthType authType = AuthType.API_KEY;

    @Column(name = "encrypted_api_key", columnDefinition = "TEXT")
    private String encryptedApiKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "model_config", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> modelConfig = new HashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "llm_type", nullable = false, length = 20)
    @Builder.Default
    private LlmType llmType = LlmType.CLIENT;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

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
