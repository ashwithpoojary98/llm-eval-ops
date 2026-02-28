package io.github.ashwithpoojary98.llmops_eval.dataset.repository;

import io.github.ashwithpoojary98.llmops_eval.dataset.entity.Dataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, UUID> {

    boolean existsByProjectIdAndName(UUID projectId, String name);

    Page<Dataset> findByProjectIdAndIsActiveTrue(UUID projectId, Pageable pageable);

    Page<Dataset> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT d FROM Dataset d LEFT JOIN FETCH d.project LEFT JOIN FETCH d.createdBy WHERE d.id = :id")
    Optional<Dataset> findByIdWithProject(@Param("id") UUID id);

    @Query("SELECT d FROM Dataset d LEFT JOIN FETCH d.project LEFT JOIN FETCH d.createdBy LEFT JOIN FETCH d.testCases tc LEFT JOIN FETCH tc.testCase WHERE d.id = :id")
    Optional<Dataset> findByIdWithTestCases(@Param("id") UUID id);

    @Query("""
            SELECT d FROM Dataset d
            WHERE d.project.id = :projectId
            AND d.isActive = true
            AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Dataset> searchByProjectId(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT d FROM Dataset d
            WHERE d.project.id = :projectId
            AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Dataset> searchByProjectIdIncludeInactive(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByProjectIdAndIsActiveTrue(UUID projectId);

    // Methods for listing across multiple projects
    Page<Dataset> findByProjectIdInAndIsActiveTrue(List<UUID> projectIds, Pageable pageable);

    Page<Dataset> findByProjectIdIn(List<UUID> projectIds, Pageable pageable);

    @Query("""
            SELECT d FROM Dataset d
            WHERE d.project.id IN :projectIds
            AND d.isActive = true
            AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Dataset> searchByProjectIds(
            @Param("projectIds") List<UUID> projectIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT d FROM Dataset d
            WHERE d.project.id IN :projectIds
            AND (LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Dataset> searchByProjectIdsIncludeInactive(
            @Param("projectIds") List<UUID> projectIds,
            @Param("search") String search,
            Pageable pageable
    );
}
