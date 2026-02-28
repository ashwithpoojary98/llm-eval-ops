package io.github.ashwithpoojary98.llmops_eval.llms.repository;

import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.ProviderType;
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
public interface LlmEndpointRepository extends JpaRepository<LlmEndpoint, UUID> {

    boolean existsByProjectIdAndName(UUID projectId, String name);

    Page<LlmEndpoint> findByProjectIdAndIsActiveTrue(UUID projectId, Pageable pageable);

    Page<LlmEndpoint> findByProjectId(UUID projectId, Pageable pageable);

    List<LlmEndpoint> findByProjectIdAndIsActiveTrue(UUID projectId);

    Optional<LlmEndpoint> findByProjectIdAndIsDefaultTrue(UUID projectId);

    @Query("SELECT e FROM LlmEndpoint e " +
           "LEFT JOIN FETCH e.project " +
           "LEFT JOIN FETCH e.createdBy " +
           "WHERE e.id = :id")
    Optional<LlmEndpoint> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT e FROM LlmEndpoint e " +
           "WHERE e.project.id = :projectId " +
           "AND e.isActive = true " +
           "AND e.providerType = :providerType")
    List<LlmEndpoint> findByProjectIdAndProviderType(
            @Param("projectId") UUID projectId,
            @Param("providerType") ProviderType providerType);

    @Query("""
            SELECT e FROM LlmEndpoint e
            WHERE e.project.id = :projectId
            AND e.isActive = true
            AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LlmEndpoint> searchByProjectId(
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            Pageable pageable);

    long countByProjectIdAndIsActiveTrue(UUID projectId);

    // Methods for filtering by LLM type
    Page<LlmEndpoint> findByProjectIdAndLlmTypeAndIsActiveTrue(UUID projectId, LlmType llmType, Pageable pageable);

    Page<LlmEndpoint> findByProjectIdAndLlmType(UUID projectId, LlmType llmType, Pageable pageable);

    List<LlmEndpoint> findByProjectIdAndLlmTypeAndIsActiveTrue(UUID projectId, LlmType llmType);

    Optional<LlmEndpoint> findByProjectIdAndLlmTypeAndIsDefaultTrue(UUID projectId, LlmType llmType);

    @Query("""
            SELECT e FROM LlmEndpoint e
            WHERE e.project.id = :projectId
            AND e.llmType = :llmType
            AND e.isActive = true
            AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LlmEndpoint> searchByProjectIdAndLlmType(
            @Param("projectId") UUID projectId,
            @Param("llmType") LlmType llmType,
            @Param("search") String search,
            Pageable pageable);

    // Global queries for accessing endpoints across all accessible projects
    @Query("SELECT e FROM LlmEndpoint e WHERE e.project.id IN :projectIds AND e.isActive = true AND e.llmType = :llmType")
    Page<LlmEndpoint> findByProjectIdInAndLlmTypeAndIsActiveTrue(
            @Param("projectIds") List<UUID> projectIds,
            @Param("llmType") LlmType llmType,
            Pageable pageable);

    @Query("""
            SELECT e FROM LlmEndpoint e
            WHERE e.project.id IN :projectIds
            AND e.llmType = :llmType
            AND e.isActive = true
            AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(e.modelName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<LlmEndpoint> searchByProjectIdInAndLlmType(
            @Param("projectIds") List<UUID> projectIds,
            @Param("llmType") LlmType llmType,
            @Param("search") String search,
            Pageable pageable);
}
