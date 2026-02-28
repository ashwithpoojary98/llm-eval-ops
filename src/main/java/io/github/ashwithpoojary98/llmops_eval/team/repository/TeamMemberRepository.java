package io.github.ashwithpoojary98.llmops_eval.team.repository;

import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMember;
import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    List<TeamMember> findByTeamId(UUID teamId);

    List<TeamMember> findByUserId(UUID userId);

    @Query("SELECT m FROM TeamMember m WHERE m.team.id = :teamId AND m.role = :role")
    List<TeamMember> findByTeamIdAndRole(@Param("teamId") UUID teamId, @Param("role") TeamMemberRole role);

    @Modifying
    @Query("DELETE FROM TeamMember m WHERE m.team.id = :teamId AND m.user.id = :userId")
    int deleteByTeamIdAndUserId(@Param("teamId") UUID teamId, @Param("userId") UUID userId);

    @Query("SELECT COUNT(m) FROM TeamMember m WHERE m.team.id = :teamId")
    long countByTeamId(@Param("teamId") UUID teamId);

    @Query("SELECT COUNT(m) FROM TeamMember m WHERE m.team.id = :teamId AND m.role = 'LEAD'")
    long countLeadsByTeamId(@Param("teamId") UUID teamId);
}
