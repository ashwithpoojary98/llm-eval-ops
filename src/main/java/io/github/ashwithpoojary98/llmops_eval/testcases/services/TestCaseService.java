package io.github.ashwithpoojary98.llmops_eval.testcases.services;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.CreateTestCaseRequest;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseResponse;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.TestCaseSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.testcases.dto.UpdateTestCaseRequest;
import io.github.ashwithpoojary98.llmops_eval.testcases.entity.TestCase;
import io.github.ashwithpoojary98.llmops_eval.testcases.exception.TestCaseNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.testcases.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public TestCaseResponse createTestCase(UUID projectId, CreateTestCaseRequest request, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!projectAccessService.canWrite(projectId, user)) {
            throw new ForbiddenException("You don't have permission to create test cases in this project");
        }

        TestCase testCase = TestCase.builder()
                .project(project)
                .question(request.getQuestion())
                .llmOutput(request.getLlmOutput())
                .retrievedContext(request.getRetrievedContext())
                .groundTruth(request.getGroundTruth())
                .createdBy(user)
                .build();

        testCase = testCaseRepository.save(testCase);

        log.info("Test case created: {} in project {} by {}", testCase.getId(), project.getName(), user.getEmail());

        return toTestCaseResponse(testCase);
    }

    @Transactional(readOnly = true)
    public TestCaseResponse getTestCase(UUID id, User user) {
        TestCase testCase = testCaseRepository.findByIdWithProject(id)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found"));

        if (!projectAccessService.canRead(testCase.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have access to this test case");
        }

        return toTestCaseResponse(testCase);
    }

    @Transactional(readOnly = true)
    public Page<TestCaseSummaryResponse> listTestCases(
            UUID projectId,
            boolean includeInactive,
            String search,
            User user,
            Pageable pageable
    ) {
        if (!projectAccessService.canRead(projectId, user)) {
            throw new ForbiddenException("You don't have access to this project");
        }

        Page<TestCase> testCases;

        if (search != null && !search.isBlank()) {
            if (includeInactive) {
                testCases = testCaseRepository.searchByProjectIdIncludeInactive(projectId, search, pageable);
            } else {
                testCases = testCaseRepository.searchByProjectId(projectId, search, pageable);
            }
        } else {
            if (includeInactive) {
                testCases = testCaseRepository.findByProjectId(projectId, pageable);
            } else {
                testCases = testCaseRepository.findByProjectIdAndIsActiveTrue(projectId, pageable);
            }
        }

        return testCases.map(this::toTestCaseSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<TestCaseResponse> listAllTestCases(
            boolean includeInactive,
            String search,
            User user,
            Pageable pageable
    ) {
        // Get all projects user has access to
        var accessibleProjectIds = projectAccessService.getAccessibleProjectIds(user);

        Page<TestCase> testCases;

        if (search != null && !search.isBlank()) {
            if (includeInactive) {
                testCases = testCaseRepository.searchByProjectIdsIncludeInactive(accessibleProjectIds, search, pageable);
            } else {
                testCases = testCaseRepository.searchByProjectIds(accessibleProjectIds, search, pageable);
            }
        } else {
            if (includeInactive) {
                testCases = testCaseRepository.findByProjectIdIn(accessibleProjectIds, pageable);
            } else {
                testCases = testCaseRepository.findByProjectIdInAndIsActiveTrue(accessibleProjectIds, pageable);
            }
        }

        return testCases.map(this::toTestCaseResponse);
    }

    @Transactional
    public TestCaseResponse updateTestCase(UUID id, UpdateTestCaseRequest request, User user) {
        TestCase testCase = testCaseRepository.findByIdWithProject(id)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found"));

        if (!projectAccessService.canWrite(testCase.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to update this test case");
        }

        if (request.getQuestion() != null && !request.getQuestion().isBlank()) {
            testCase.setQuestion(request.getQuestion());
        }
        if (request.getLlmOutput() != null) {
            testCase.setLlmOutput(request.getLlmOutput());
        }
        if (request.getRetrievedContext() != null) {
            testCase.setRetrievedContext(request.getRetrievedContext());
        }
        if (request.getGroundTruth() != null) {
            testCase.setGroundTruth(request.getGroundTruth());
        }

        testCase = testCaseRepository.save(testCase);

        log.info("Test case updated: {} by {}", testCase.getId(), user.getEmail());

        return toTestCaseResponse(testCase);
    }

    @Transactional
    public void deleteTestCase(UUID id, User user) {
        TestCase testCase = testCaseRepository.findByIdWithProject(id)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found"));

        if (!projectAccessService.canAdmin(testCase.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to delete this test case");
        }

        testCase.setIsActive(false);
        testCaseRepository.save(testCase);

        log.info("Test case soft deleted: {} by {}", testCase.getId(), user.getEmail());
    }

    @Transactional
    public TestCaseResponse reactivateTestCase(UUID id, User user) {
        TestCase testCase = testCaseRepository.findByIdWithProject(id)
                .orElseThrow(() -> new TestCaseNotFoundException("Test case not found"));

        if (!projectAccessService.canAdmin(testCase.getProject().getId(), user)) {
            throw new ForbiddenException("You don't have permission to reactivate this test case");
        }

        testCase.setIsActive(true);
        testCase = testCaseRepository.save(testCase);

        log.info("Test case reactivated: {} by {}", testCase.getId(), user.getEmail());

        return toTestCaseResponse(testCase);
    }

    private TestCaseResponse toTestCaseResponse(TestCase testCase) {
        TestCaseResponse.TestCaseResponseBuilder builder = TestCaseResponse.builder()
                .id(testCase.getId().toString())
                .projectId(testCase.getProject().getId().toString())
                .projectName(testCase.getProject().getName())
                .question(testCase.getQuestion())
                .llmOutput(testCase.getLlmOutput())
                .retrievedContext(testCase.getRetrievedContext())
                .groundTruth(testCase.getGroundTruth())
                .isActive(testCase.getIsActive())
                .createdAt(testCase.getCreatedAt())
                .updatedAt(testCase.getUpdatedAt());

        if (testCase.getCreatedBy() != null) {
            builder.createdById(testCase.getCreatedBy().getId().toString())
                    .createdByName(testCase.getCreatedBy().getFullName());
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
