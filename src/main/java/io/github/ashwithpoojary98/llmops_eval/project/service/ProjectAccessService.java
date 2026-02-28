package io.github.ashwithpoojary98.llmops_eval.project.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.project.entity.AccessLevel;
import io.github.ashwithpoojary98.llmops_eval.project.entity.Project;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectMemberRepository;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectTeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get the highest access level for a user on a project.
     * Considers both direct membership and team membership.
     */
    @Transactional(readOnly = true)
    public Optional<AccessLevel> getUserAccessLevel(UUID projectId, User user) {
        // System admins have full access
        if (user.getRole() == LLMOPsRole.ADMIN) {
            return Optional.of(AccessLevel.ADMIN);
        }

        // Check direct membership first
        Optional<AccessLevel> directAccess = projectMemberRepository
                .findAccessLevelByProjectAndUser(projectId, user.getId());

        // Check team membership
        List<AccessLevel> teamAccess = projectTeamRepository
                .findAccessLevelsByProjectAndUser(projectId, user.getId());

        // Return highest access level
        AccessLevel highest = null;

        if (directAccess.isPresent()) {
            highest = directAccess.get();
        }

        for (AccessLevel level : teamAccess) {
            if (highest == null || level.getPriority() > highest.getPriority()) {
                highest = level;
            }
        }

        return Optional.ofNullable(highest);
    }

    /**
     * Check if user has at least the specified access level
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(UUID projectId, User user, AccessLevel requiredLevel) {
        return getUserAccessLevel(projectId, user)
                .map(level -> level.getPriority() >= requiredLevel.getPriority())
                .orElse(false);
    }

    /**
     * Check if user can read the project
     */
    @Transactional(readOnly = true)
    public boolean canRead(UUID projectId, User user) {
        return hasAccess(projectId, user, AccessLevel.READ);
    }

    /**
     * Check if user can write to the project
     */
    @Transactional(readOnly = true)
    public boolean canWrite(UUID projectId, User user) {
        return hasAccess(projectId, user, AccessLevel.WRITE);
    }

    /**
     * Check if user can admin the project
     */
    @Transactional(readOnly = true)
    public boolean canAdmin(UUID projectId, User user) {
        return hasAccess(projectId, user, AccessLevel.ADMIN);
    }

    /**
     * Check if user is the project creator
     */
    public boolean isCreator(Project project, User user) {
        return project.getCreatedBy() != null &&
               project.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Get all project IDs the user has access to
     */
    @Transactional(readOnly = true)
    public List<UUID> getAccessibleProjectIds(User user) {
        // System admins have access to all projects
        if (user.getRole() == LLMOPsRole.ADMIN) {
            return projectRepository.findAll().stream()
                    .map(Project::getId)
                    .collect(Collectors.toList());
        }

        // Get projects from direct membership
        List<UUID> directProjects = projectMemberRepository.findProjectIdsByUserId(user.getId());

        // Get projects from team membership
        List<UUID> teamProjects = projectTeamRepository.findProjectIdsByUserId(user.getId());

        // Combine and deduplicate
        return java.util.stream.Stream.concat(directProjects.stream(), teamProjects.stream())
                .distinct()
                .collect(Collectors.toList());
    }
}
