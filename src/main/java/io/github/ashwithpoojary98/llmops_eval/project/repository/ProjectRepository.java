package io.github.ashwithpoojary98.llmops_eval.project.repository;

import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.entity.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    boolean existsByName(String name);

    Optional<Project> findByName(String name);

    @EntityGraph(attributePaths = {
            "createdBy",
            "teams", "teams.team", "teams.assignedBy",
            "members", "members.user", "members.assignedBy"
    })
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithDetails(@Param("id") UUID id);

    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Project p
            WHERE p.status = :status
            AND (
                p.createdBy.id = :userId
                OR EXISTS (SELECT pm FROM ProjectMember pm WHERE pm.project = p AND pm.user.id = :userId)
                OR EXISTS (SELECT pt FROM ProjectTeam pt JOIN pt.team.members tm WHERE pt.project = p AND tm.user.id = :userId)
            )
            """)
    Page<Project> findAccessibleByUser(@Param("userId") UUID userId, @Param("status") ProjectStatus status, Pageable pageable);

    @Query("""
            SELECT DISTINCT p FROM Project p
            WHERE (
                p.createdBy.id = :userId
                OR EXISTS (SELECT pm FROM ProjectMember pm WHERE pm.project = p AND pm.user.id = :userId)
                OR EXISTS (SELECT pt FROM ProjectTeam pt JOIN pt.team.members tm WHERE pt.project = p AND tm.user.id = :userId)
            )
            """)
    Page<Project> findAllAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            SELECT p FROM Project p
            WHERE p.status = :status
            AND (LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Project> searchByNameOrDescription(@Param("search") String search, @Param("status") ProjectStatus status, Pageable pageable);
}
