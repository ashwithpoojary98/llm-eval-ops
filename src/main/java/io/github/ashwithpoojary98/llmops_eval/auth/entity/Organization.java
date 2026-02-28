package io.github.ashwithpoojary98.llmops_eval.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "allowed_domains", columnDefinition = "TEXT[]", nullable = false)
    @JdbcTypeCode(SqlTypes.ARRAY)
    @Builder.Default
    private List<String> allowedDomains = List.of();

    @Column(name = "sso_enabled")
    @Builder.Default
    private Boolean ssoEnabled = false;

    @Column(name = "sso_provider")
    private String ssoProvider;

    @Column(name = "sso_config", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> ssoConfig;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
