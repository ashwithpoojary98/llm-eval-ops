package io.github.ashwithpoojary98.llmops_eval.dataset.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ConflictException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.AddTestCasesRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.CreateDatasetRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.DatasetResponse;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.DatasetSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.RemoveTestCasesRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.dto.UpdateDatasetRequest;
import io.github.ashwithpoojary98.llmops_eval.dataset.entity.Dataset;
import io.github.ashwithpoojary98.llmops_eval.dataset.entity.DatasetTestCase;
import io.github.ashwithpoojary98.llmops_eval.dataset.exception.DatasetNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.dataset.repository.DatasetRepository;
import io.github.ashwithpoojary98.llmops_eval.dataset.repository.DatasetTestCaseRepository;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.testcases.entity.TestCase;
import io.github.ashwithpoojary98.llmops_eval.testcases.exception.TestCaseNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.testcases.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final DatasetTestCaseRepository datasetTestCaseRepository;
    private final ProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public DatasetResponse createDataset(UUID projectId, CreateDatasetRequest request, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!projectAccessService.canWrite(projectId, user)) {
            throw new ForbiddenException("You don't have permission to create datasets in this project");
        }

        if (datasetRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new ConflictException("Dataset with this name already exists in the project");
        }

        Dataset dataset = Dataset.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(user)
                .build();

        dataset = datasetRepository.save(dataset);

        // Add initial test cases if provided
        if (request.getTestCaseIds() != null && !request.getTestCaseIds().isEmpty()) {
            addTestCasesToDataset(dataset, request.getTestCaseIds(), user);
        }

        log.info("Dataset created: {} in project {} by {}", dataset.getName(), project.getName(), user.getEmail());

        return toDatasetResponse(dataset, true);
    }

    @Transactional(readOnly = true)
    public DatasetResponse getDataset(UUID id, User user, boolean includeTestCases) {
        Dataset dataset;
        if (includeTestCases) {
            dataset = datasetRepository.findByIdWithTestCases(id)
                    .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));
        } else {
            dataset = datasetRepository.findByIdWithProject(id)
                    .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));
        }

        if (!projectAccessService.canRead(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have access to this dataset");
        }

        return toDatasetResponse(dataset, includeTestCases);
    }

    @Transactional(readOnly = true)
    public Page<DatasetSummaryResponse> listDatasets(
            UUID projectId,
            boolean includeInactive,
            String search,
            User user,
            Pageable pageable
    ) {
        if (!projectAccessService.canRead(projectId, user)) {
            throw new ForbiddenException("You don't have access to this project");
        }

        Page<Dataset> datasets;

        if (search != null && !search.isBlank()) {
            if (includeInactive) {
                datasets = datasetRepository.searchByProjectIdIncludeInactive(projectId, search, pageable);
            } else {
                datasets = datasetRepository.searchByProjectId(projectId, search, pageable);
            }
        } else {
            if (includeInactive) {
                datasets = datasetRepository.findByProjectId(projectId, pageable);
            } else {
                datasets = datasetRepository.findByProjectIdAndIsActiveTrue(projectId, pageable);
            }
        }

        return datasets.map(this::toDatasetSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<DatasetSummaryResponse> listAllDatasets(
            boolean includeInactive,
            String search,
            User user,
            Pageable pageable
    ) {
        var accessibleProjectIds = projectAccessService.getAccessibleProjectIds(user);

        Page<Dataset> datasets;

        if (search != null && !search.isBlank()) {
            if (includeInactive) {
                datasets = datasetRepository.searchByProjectIdsIncludeInactive(accessibleProjectIds, search, pageable);
            } else {
                datasets = datasetRepository.searchByProjectIds(accessibleProjectIds, search, pageable);
            }
        } else {
            if (includeInactive) {
                datasets = datasetRepository.findByProjectIdIn(accessibleProjectIds, pageable);
            } else {
                datasets = datasetRepository.findByProjectIdInAndIsActiveTrue(accessibleProjectIds, pageable);
            }
        }

        return datasets.map(this::toDatasetSummaryResponse);
    }

    @Transactional
    public DatasetResponse updateDataset(UUID id, UpdateDatasetRequest request, User user) {
        Dataset dataset = datasetRepository.findByIdWithProject(id)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        if (!projectAccessService.canWrite(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to update this dataset");
        }

        if (request.getName() != null && !request.getName().isBlank()) {
            if (!request.getName().equals(dataset.getName()) &&
                datasetRepository.existsByProjectIdAndName(dataset.getProject().getId(), request.getName())) {
                throw new ConflictException("Dataset with this name already exists in the project");
            }
            dataset.setName(request.getName());
        }

        if (request.getDescription() != null) {
            dataset.setDescription(request.getDescription());
        }

        dataset = datasetRepository.save(dataset);

        log.info("Dataset updated: {} by {}", dataset.getName(), user.getEmail());

        return toDatasetResponse(dataset, false);
    }

    @Transactional
    public void deleteDataset(UUID id, User user) {
        Dataset dataset = datasetRepository.findByIdWithProject(id)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        if (!projectAccessService.canAdmin(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to delete this dataset");
        }

        dataset.setIsActive(false);
        datasetRepository.save(dataset);

        log.info("Dataset soft deleted: {} by {}", dataset.getName(), user.getEmail());
    }

    @Transactional
    public DatasetResponse reactivateDataset(UUID id, User user) {
        Dataset dataset = datasetRepository.findByIdWithProject(id)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        if (!projectAccessService.canAdmin(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to reactivate this dataset");
        }

        dataset.setIsActive(true);
        dataset = datasetRepository.save(dataset);

        log.info("Dataset reactivated: {} by {}", dataset.getName(), user.getEmail());

        return toDatasetResponse(dataset, false);
    }

    @Transactional
    public DatasetResponse addTestCases(UUID datasetId, AddTestCasesRequest request, User user) {
        Dataset dataset = datasetRepository.findByIdWithTestCases(datasetId)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        if (!projectAccessService.canWrite(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to modify this dataset");
        }

        addTestCasesToDataset(dataset, request.getTestCaseIds(), user);

        log.info("Test cases added to dataset: {} by {}", dataset.getName(), user.getEmail());

        return toDatasetResponse(dataset, true);
    }

    @Transactional
    public DatasetResponse removeTestCases(UUID datasetId, RemoveTestCasesRequest request, User user) {
        Dataset dataset = datasetRepository.findByIdWithTestCases(datasetId)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        if (!projectAccessService.canWrite(dataset.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to modify this dataset");
        }

        datasetTestCaseRepository.deleteByDatasetIdAndTestCaseIdIn(datasetId, request.getTestCaseIds());

        // Refresh the dataset to get updated test cases
        dataset = datasetRepository.findByIdWithTestCases(datasetId)
                .orElseThrow(() -> new DatasetNotFoundException("Dataset not found"));

        log.info("Test cases removed from dataset: {} by {}", dataset.getName(), user.getEmail());

        return toDatasetResponse(dataset, true);
    }

    private void addTestCasesToDataset(Dataset dataset, List<UUID> testCaseIds, User user) {
        for (UUID testCaseId : testCaseIds) {
            if (datasetTestCaseRepository.existsByDatasetIdAndTestCaseId(dataset.getId(), testCaseId)) {
                continue; // Skip if already exists
            }

            TestCase testCase = testCaseRepository.findById(testCaseId)
                    .orElseThrow(() -> new TestCaseNotFoundException("Test case not found: " + testCaseId));

            // Verify test case belongs to the same project
            if (!testCase.getProject().getId().equals(dataset.getProject().getId())) {
                throw new ForbiddenException("Test case does not belong to the same project as the dataset");
            }

            DatasetTestCase datasetTestCase = DatasetTestCase.builder()
                    .dataset(dataset)
                    .testCase(testCase)
                    .addedBy(user)
                    .build();

            datasetTestCaseRepository.save(datasetTestCase);
            dataset.getTestCases().add(datasetTestCase);
        }
    }

    private DatasetResponse toDatasetResponse(Dataset dataset, boolean includeTestCases) {
        DatasetResponse.DatasetResponseBuilder builder = DatasetResponse.builder()
                .id(dataset.getId().toString())
                .projectId(dataset.getProject().getId().toString())
                .projectName(dataset.getProject().getName())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .isActive(dataset.getIsActive())
                .testCaseCount(dataset.getTestCases() != null ? dataset.getTestCases().size() : 0)
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt());

        if (dataset.getCreatedBy() != null) {
            builder.createdById(dataset.getCreatedBy().getId().toString())
                    .createdByName(dataset.getCreatedBy().getFullName());
        }

        if (includeTestCases && dataset.getTestCases() != null) {
            List<TestCaseSummaryResponse> testCases = dataset.getTestCases().stream()
                    .map(dtc -> toTestCaseSummaryResponse(dtc.getTestCase()))
                    .toList();
            builder.testCases(testCases);
        }

        return builder.build();
    }

    private DatasetSummaryResponse toDatasetSummaryResponse(Dataset dataset) {
        DatasetSummaryResponse.DatasetSummaryResponseBuilder builder = DatasetSummaryResponse.builder()
                .id(dataset.getId().toString())
                .projectId(dataset.getProject().getId().toString())
                .name(dataset.getName())
                .description(dataset.getDescription())
                .isActive(dataset.getIsActive())
                .testCaseCount((int) datasetTestCaseRepository.countByDatasetId(dataset.getId()))
                .createdAt(dataset.getCreatedAt())
                .updatedAt(dataset.getUpdatedAt());

        if (dataset.getCreatedBy() != null) {
            builder.createdByName(dataset.getCreatedBy().getFullName());
        }

        return builder.build();
    }

    private TestCaseSummaryResponse toTestCaseSummaryResponse(TestCase testCase) {
        TestCaseSummaryResponse.TestCaseSummaryResponseBuilder builder = TestCaseSummaryResponse.builder()
                .id(testCase.getId().toString())
                .projectId(testCase.getProject().getId().toString())
                .question(testCase.getQuestion())
                .groundTruth(testCase.getGroundTruth())
                .isActive(testCase.getIsActive())
                .createdAt(testCase.getCreatedAt())
                .updatedAt(testCase.getUpdatedAt());

        if (testCase.getCreatedBy() != null) {
            builder.createdByName(testCase.getCreatedBy().getFullName());
        }

        return builder.build();
    }
}
