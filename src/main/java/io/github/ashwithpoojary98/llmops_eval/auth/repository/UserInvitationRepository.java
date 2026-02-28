package io.github.ashwithpoojary98.llmops_eval.auth.repository;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.UserInvitation;
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
public interface UserInvitationRepository extends JpaRepository<UserInvitation, UUID> {

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.tokenHash = :tokenHash
              AND ui.acceptedAt IS NULL
              AND ui.revokedAt IS NULL
              AND ui.expiresAt > :now
            """)
    Optional<UserInvitation> findValidByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            WHERE ui.email = :email
              AND ui.organization.id = :orgId
              AND ui.acceptedAt IS NULL
              AND ui.revokedAt IS NULL
              AND ui.expiresAt > :now
            """)
    Optional<UserInvitation> findPendingByEmailAndOrg(
            @Param("email") String email,
            @Param("orgId") UUID orgId,
            @Param("now") Instant now
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.organization.id = :orgId
              AND ui.acceptedAt IS NULL
              AND ui.revokedAt IS NULL
              AND ui.expiresAt > :now
            ORDER BY ui.createdAt DESC
            """)
    Page<UserInvitation> findPendingByOrganization(
            @Param("orgId") UUID orgId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.organization.id = :orgId
              AND ui.acceptedAt IS NULL
              AND ui.revokedAt IS NULL
              AND ui.expiresAt <= :now
            ORDER BY ui.createdAt DESC
            """)
    Page<UserInvitation> findExpiredByOrganization(
            @Param("orgId") UUID orgId,
            @Param("now") Instant now,
            Pageable pageable
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.organization.id = :orgId
              AND ui.acceptedAt IS NOT NULL
            ORDER BY ui.createdAt DESC
            """)
    Page<UserInvitation> findAcceptedByOrganization(
            @Param("orgId") UUID orgId,
            Pageable pageable
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.organization.id = :orgId
              AND ui.revokedAt IS NOT NULL
            ORDER BY ui.createdAt DESC
            """)
    Page<UserInvitation> findRevokedByOrganization(
            @Param("orgId") UUID orgId,
            Pageable pageable
    );

    @Query("""
            SELECT ui FROM UserInvitation ui
            JOIN FETCH ui.organization
            LEFT JOIN FETCH ui.invitedBy
            WHERE ui.organization.id = :orgId
            ORDER BY ui.createdAt DESC
            """)
    Page<UserInvitation> findAllByOrganization(
            @Param("orgId") UUID orgId,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE UserInvitation ui
            SET ui.revokedAt = :revokedAt
            WHERE ui.id = :id
              AND ui.acceptedAt IS NULL
              AND ui.revokedAt IS NULL
            """)
    int revokeById(@Param("id") UUID id, @Param("revokedAt") Instant revokedAt);

    @Modifying
    @Query("DELETE FROM UserInvitation ui WHERE ui.expiresAt < :now AND ui.acceptedAt IS NULL")
    int deleteExpiredInvitations(@Param("now") Instant now);
}
