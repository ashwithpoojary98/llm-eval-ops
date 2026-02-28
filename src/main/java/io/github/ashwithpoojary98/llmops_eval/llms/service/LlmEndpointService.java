package io.github.ashwithpoojary98.llmops_eval.llms.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.llms.dto.*;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmEndpoint;
import io.github.ashwithpoojary98.llmops_eval.llms.entity.LlmType;
import io.github.ashwithpoojary98.llmops_eval.llms.exception.LlmEndpointNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.llms.repository.LlmEndpointRepository;
import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectAccessService;
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
public class LlmEndpointService {

    private final LlmEndpointRepository llmEndpointRepository;
    private final ProjectRepository projectRepository;
    private final ProjectAccessService projectAccessService;
    private final EncryptionService encryptionService;

    @Transactional
    public LlmEndpointResponse createEndpoint(UUID projectId, CreateLlmEndpointRequest request, User user) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Check write access
        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(project.getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.WRITE) {
            throw new ForbiddenException("You don't have permission to create LLM endpoints in this project");
        }

        // Check for duplicate name
        if (llmEndpointRepository.existsByProjectIdAndName(projectId, request.getName())) {
            throw new IllegalArgumentException("An endpoint with this name already exists in the project");
        }

        // Encrypt API key
        String encryptedKey = encryptionService.encrypt(request.getApiKey());

        LlmEndpoint endpoint = LlmEndpoint.builder()
                .project(project)
                .name(request.getName())
                .description(request.getDescription())
                .providerType(request.getProviderType())
                .modelName(request.getModelName())
                .apiUrl(request.getApiUrl())
                .authType(request.getAuthType())
                .encryptedApiKey(encryptedKey)
                .modelConfig(request.getModelConfig())
                .llmType(request.getLlmType())
                .isDefault(request.getIsDefault())
                .createdBy(user)
                .build();

        // If this is set as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            llmEndpointRepository.findByProjectIdAndIsDefaultTrue(projectId)
                    .ifPresent(existing -> {
                        existing.setIsDefault(false);
                        llmEndpointRepository.save(existing);
                    });
        }

        endpoint = llmEndpointRepository.save(endpoint);
        log.info("Created LLM endpoint: {} in project: {}", endpoint.getId(), projectId);

        return toResponse(endpoint);
    }

    @Transactional(readOnly = true)
    public Page<LlmEndpointSummaryResponse> listEndpoints(
            UUID projectId, boolean includeInactive, String search, LlmType llmType, User user, Pageable pageable) {

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found: " + projectId));

        // Check read access
        projectAccessService.getUserAccessLevel(project.getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        Page<LlmEndpoint> endpoints;
        if (llmType != null) {
            // Filter by LLM type
            if (search != null && !search.isBlank()) {
                endpoints = llmEndpointRepository.searchByProjectIdAndLlmType(projectId, llmType, search, pageable);
            } else if (includeInactive) {
                endpoints = llmEndpointRepository.findByProjectIdAndLlmType(projectId, llmType, pageable);
            } else {
                endpoints = llmEndpointRepository.findByProjectIdAndLlmTypeAndIsActiveTrue(projectId, llmType, pageable);
            }
        } else {
            // No type filter - return all
            if (search != null && !search.isBlank()) {
                endpoints = llmEndpointRepository.searchByProjectId(projectId, search, pageable);
            } else if (includeInactive) {
                endpoints = llmEndpointRepository.findByProjectId(projectId, pageable);
            } else {
                endpoints = llmEndpointRepository.findByProjectIdAndIsActiveTrue(projectId, pageable);
            }
        }

        return endpoints.map(this::toSummaryResponse);
    }

    /**
     * List LLM endpoints across all accessible projects (for global views).
     */
    @Transactional(readOnly = true)
    public Page<LlmEndpointSummaryResponse> listEndpointsGlobal(
            LlmType llmType, String search, User user, Pageable pageable) {

        List<UUID> accessibleProjectIds = projectAccessService.getAccessibleProjectIds(user);
        if (accessibleProjectIds.isEmpty()) {
            return Page.empty(pageable);
        }

        Page<LlmEndpoint> endpoints;
        if (search != null && !search.isBlank()) {
            endpoints = llmEndpointRepository.searchByProjectIdInAndLlmType(accessibleProjectIds, llmType, search, pageable);
        } else {
            endpoints = llmEndpointRepository.findByProjectIdInAndLlmTypeAndIsActiveTrue(accessibleProjectIds, llmType, pageable);
        }

        return endpoints.map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public LlmEndpointResponse getEndpoint(UUID endpointId, User user) {
        LlmEndpoint endpoint = llmEndpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        // Check read access
        projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));

        return toResponse(endpoint);
    }

    @Transactional
    public LlmEndpointResponse updateEndpoint(UUID endpointId, UpdateLlmEndpointRequest request, User user) {
        LlmEndpoint endpoint = llmEndpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        // Check write access
        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN && accessLevel != AccessLevel.WRITE) {
            throw new ForbiddenException("You don't have permission to update this LLM endpoint");
        }

        // Update fields if provided
        if (request.getName() != null) {
            endpoint.setName(request.getName());
        }
        if (request.getDescription() != null) {
            endpoint.setDescription(request.getDescription());
        }
        if (request.getProviderType() != null) {
            endpoint.setProviderType(request.getProviderType());
        }
        if (request.getModelName() != null) {
            endpoint.setModelName(request.getModelName());
        }
        if (request.getApiUrl() != null) {
            endpoint.setApiUrl(request.getApiUrl());
        }
        if (request.getAuthType() != null) {
            endpoint.setAuthType(request.getAuthType());
        }
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            endpoint.setEncryptedApiKey(encryptionService.encrypt(request.getApiKey()));
        }
        if (request.getModelConfig() != null) {
            endpoint.setModelConfig(request.getModelConfig());
        }
        if (request.getLlmType() != null) {
            endpoint.setLlmType(request.getLlmType());
        }
        if (request.getIsDefault() != null) {
            if (Boolean.TRUE.equals(request.getIsDefault())) {
                llmEndpointRepository.findByProjectIdAndIsDefaultTrue(endpoint.getProject().getId())
                        .ifPresent(existing -> {
                            existing.setIsDefault(false);
                            llmEndpointRepository.save(existing);
                        });
            }
            endpoint.setIsDefault(request.getIsDefault());
        }

        endpoint = llmEndpointRepository.save(endpoint);
        log.info("Updated LLM endpoint: {}", endpointId);

        return toResponse(endpoint);
    }

    @Transactional
    public void deleteEndpoint(UUID endpointId, User user) {
        LlmEndpoint endpoint = llmEndpointRepository.findByIdWithDetails(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        // Check admin access
        AccessLevel accessLevel = projectAccessService.getUserAccessLevel(endpoint.getProject().getId(), user)
                .orElseThrow(() -> new ForbiddenException("You don't have access to this project"));
        if (accessLevel != AccessLevel.ADMIN) {
            throw new ForbiddenException("Only project admins can delete LLM endpoints");
        }

        // Soft delete
        endpoint.setIsActive(false);
        llmEndpointRepository.save(endpoint);
        log.info("Deleted (soft) LLM endpoint: {}", endpointId);
    }

    /**
     * Get decrypted API key for internal use (evaluation engine).
     */
    @Transactional(readOnly = true)
    public String getDecryptedApiKey(UUID endpointId) {
        LlmEndpoint endpoint = llmEndpointRepository.findById(endpointId)
                .orElseThrow(() -> new LlmEndpointNotFoundException("LLM endpoint not found: " + endpointId));

        return encryptionService.decrypt(endpoint.getEncryptedApiKey());
    }

    @Transactional(readOnly = true)
    public List<LlmEndpoint> getActiveEndpointsForProject(UUID projectId) {
        return llmEndpointRepository.findByProjectIdAndIsActiveTrue(projectId);
    }

    private LlmEndpointResponse toResponse(LlmEndpoint endpoint) {
        return LlmEndpointResponse.builder()
                .id(endpoint.getId().toString())
                .projectId(endpoint.getProject().getId().toString())
                .projectName(endpoint.getProject().getName())
                .name(endpoint.getName())
                .description(endpoint.getDescription())
                .providerType(endpoint.getProviderType())
                .modelName(endpoint.getModelName())
                .apiUrl(endpoint.getApiUrl())
                .authType(endpoint.getAuthType())
                .hasApiKey(endpoint.getEncryptedApiKey() != null && !endpoint.getEncryptedApiKey().isBlank())
                .modelConfig(endpoint.getModelConfig())
                .llmType(endpoint.getLlmType())
                .isActive(endpoint.getIsActive())
                .isDefault(endpoint.getIsDefault())
                .createdById(endpoint.getCreatedBy().getId().toString())
                .createdByName(endpoint.getCreatedBy().getFullName())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }

    private LlmEndpointSummaryResponse toSummaryResponse(LlmEndpoint endpoint) {
        return LlmEndpointSummaryResponse.builder()
                .id(endpoint.getId().toString())
                .projectId(endpoint.getProject().getId().toString())
                .projectName(endpoint.getProject().getName())
                .name(endpoint.getName())
                .providerType(endpoint.getProviderType())
                .modelName(endpoint.getModelName())
                .llmType(endpoint.getLlmType())
                .isActive(endpoint.getIsActive())
                .isDefault(endpoint.getIsDefault())
                .createdAt(endpoint.getCreatedAt())
                .build();
    }
}
