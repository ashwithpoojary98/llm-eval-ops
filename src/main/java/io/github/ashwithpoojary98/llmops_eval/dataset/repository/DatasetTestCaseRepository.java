package io.github.ashwithpoojary98.llmops_eval.dataset.repository;

import io.github.ashwithpoojary98.llmops_eval.dataset.entity.DatasetTestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DatasetTestCaseRepository extends JpaRepository<DatasetTestCase, UUID> {

    boolean existsByDatasetIdAndTestCaseId(UUID datasetId, UUID testCaseId);

    List<DatasetTestCase> findByDatasetId(UUID datasetId);

    @Modifying
    @Query("DELETE FROM DatasetTestCase dtc WHERE dtc.dataset.id = :datasetId AND dtc.testCase.id IN :testCaseIds")
    void deleteByDatasetIdAndTestCaseIdIn(@Param("datasetId") UUID datasetId, @Param("testCaseIds") List<UUID> testCaseIds);

    long countByDatasetId(UUID datasetId);
}
