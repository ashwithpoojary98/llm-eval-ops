package io.github.ashwithpoojary98.llmops_eval.project.repository;

import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import io.github.ashwithpoojary98.llmops_eval.project.entity.ProjectTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectTeamRepository extends JpaRepository<ProjectTeam, UUID> {

    Optional<ProjectTeam> findByProjectIdAndTeamId(UUID projectId, UUID teamId);

    boolean existsByProjectIdAndTeamId(UUID projectId, UUID teamId);

    List<ProjectTeam> findByProjectId(UUID projectId);

    List<ProjectTeam> findByTeamId(UUID teamId);

    @Query("""
            SELECT pt.accessLevel FROM ProjectTeam pt
            JOIN pt.team.members tm
            WHERE pt.project.id = :projectId AND tm.user.id = :userId
            ORDER BY pt.accessLevel DESC
            """)
    List<AccessLevel> findAccessLevelsByProjectAndUser(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    void deleteByProjectIdAndTeamId(UUID projectId, UUID teamId);

    @Query("""
            SELECT DISTINCT pt.project.id FROM ProjectTeam pt
            JOIN pt.team.members tm
            WHERE tm.user.id = :userId
            """)
    List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);
}
