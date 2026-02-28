package io.github.ashwithpoojary98.llmops_eval.testcases.repository;

import io.github.ashwithpoojary98.llmops_eval.testcases.entity.TestCase;
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
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    Page<TestCase> findByProjectIdAndIsActiveTrue(UUID projectId, Pageable pageable);

    Page<TestCase> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT tc FROM TestCase tc LEFT JOIN FETCH tc.project LEFT JOIN FETCH tc.createdBy WHERE tc.id = :id")
    Optional<TestCase> findByIdWithProject(@Param("id") UUID id);

    @Query("""
            SELECT tc FROM TestCase tc
            WHERE tc.project.id = :projectId
            AND tc.isActive = true
            AND LOWER(tc.question) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<TestCase> searchByProjectId(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT tc FROM TestCase tc
            WHERE tc.project.id = :projectId
            AND LOWER(tc.question) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<TestCase> searchByProjectIdIncludeInactive(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable
    );

    long countByProjectIdAndIsActiveTrue(UUID projectId);

    // Methods for listing across multiple projects
    Page<TestCase> findByProjectIdInAndIsActiveTrue(List<UUID> projectIds, Pageable pageable);

    Page<TestCase> findByProjectIdIn(List<UUID> projectIds, Pageable pageable);

    @Query("""
            SELECT tc FROM TestCase tc
            WHERE tc.project.id IN :projectIds
            AND tc.isActive = true
            AND LOWER(tc.question) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<TestCase> searchByProjectIds(
            @Param("projectIds") List<UUID> projectIds,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
            SELECT tc FROM TestCase tc
            WHERE tc.project.id IN :projectIds
            AND LOWER(tc.question) LIKE LOWER(CONCAT('%', :search, '%'))
            """)
    Page<TestCase> searchByProjectIdsIncludeInactive(
            @Param("projectIds") List<UUID> projectIds,
            @Param("search") String search,
            Pageable pageable
    );
}
