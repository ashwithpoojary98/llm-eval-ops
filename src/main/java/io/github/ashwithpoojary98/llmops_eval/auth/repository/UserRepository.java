package io.github.ashwithpoojary98.llmops_eval.auth.repository;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndOrganization(String email, Organization organization);

    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.email = :email AND u.isActive = true")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.id = :id AND u.isActive = true")
    Optional<User> findActiveById(@Param("id") UUID id);

    boolean existsByEmailAndOrganization(String email, Organization organization);

    boolean existsByRole(LLMOPsRole role);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") UUID userId, @Param("lastLoginAt") Instant lastLoginAt);

    @Query("""
            SELECT u FROM User u
            WHERE u.organization.id = :organizationId
            ORDER BY u.createdAt DESC
            """)
    Page<User> findByOrganizationId(@Param("organizationId") UUID organizationId, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE u.organization.id = :organizationId AND u.isActive = :isActive
            ORDER BY u.createdAt DESC
            """)
    Page<User> findByOrganizationIdAndIsActive(
            @Param("organizationId") UUID organizationId,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}
