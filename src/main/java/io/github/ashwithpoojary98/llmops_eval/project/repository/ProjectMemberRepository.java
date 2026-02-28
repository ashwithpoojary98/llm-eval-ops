package io.github.ashwithpoojary98.llmops_eval.project.repository;

import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import io.github.ashwithpoojary98.llmops_eval.project.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, UUID> {

    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);

    boolean existsByProjectIdAndUserId(UUID projectId, UUID userId);

    List<ProjectMember> findByProjectId(UUID projectId);

    List<ProjectMember> findByUserId(UUID userId);

    @Query("SELECT pm.accessLevel FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<AccessLevel> findAccessLevelByProjectAndUser(@Param("projectId") UUID projectId, @Param("userId") UUID userId);

    void deleteByProjectIdAndUserId(UUID projectId, UUID userId);

    @Query("SELECT COUNT(pm) FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.accessLevel = 'ADMIN'")
    long countAdminsByProjectId(@Param("projectId") UUID projectId);

    @Query("SELECT pm.project.id FROM ProjectMember pm WHERE pm.user.id = :userId")
    List<UUID> findProjectIdsByUserId(@Param("userId") UUID userId);
}
