package io.github.ashwithpoojary98.llmops_eval.auth.repository;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Query("""
                SELECT rt
                FROM RefreshToken rt
                JOIN FETCH rt.user u
                WHERE rt.tokenHash = :tokenHash
                  AND rt.revokedAt IS NULL
                  AND rt.expiresAt > :now
            """)
    Optional<RefreshToken> findValidByTokenHash(
            @Param("tokenHash") String tokenHash,
            @Param("now") Instant now
    );

    @Modifying
    @Query("""
                UPDATE RefreshToken rt
                SET rt.revokedAt = :revokedAt
                WHERE rt.user.id = :userId
                  AND rt.revokedAt IS NULL
            """)
    int revokeAllByUserId(
            @Param("userId") UUID userId,
            @Param("revokedAt") Instant revokedAt
    );

    @Modifying
    @Query("""
                UPDATE RefreshToken rt
                SET rt.revokedAt = :revokedAt
                WHERE rt.user.id = :userId
                  AND rt.tokenHash = :tokenHash
                  AND rt.revokedAt IS NULL
            """)
    int revokeByUserIdAndTokenHash(
            @Param("userId") UUID userId,
            @Param("tokenHash") String tokenHash,
            @Param("revokedAt") Instant revokedAt
    );


    @Modifying
    @Query("""
                DELETE FROM RefreshToken rt
                WHERE rt.expiresAt < :now
            """)
    int deleteExpiredTokens(@Param("now") Instant now);
}

