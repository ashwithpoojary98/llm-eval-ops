package io.github.ashwithpoojary98.llmops_eval.project.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ConflictException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.UserNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import io.github.ashwithpoojary98.llmops_eval.project.dto.*;
import io.github.ashwithpoojary98.llmops_eval.project.entity.*;
import io.github.ashwithpoojary98.llmops_eval.project.exception.ProjectNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectMemberRepository;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectRepository;
import io.github.ashwithpoojary98.llmops_eval.project.repository.ProjectTeamRepository;
import io.github.ashwithpoojary98.llmops_eval.team.entity.Team;
import io.github.ashwithpoojary98.llmops_eval.team.exception.TeamNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ProjectAccessService accessService;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, User createdBy) {
        if (projectRepository.existsByName(request.getName())) {
            throw new ConflictException("Project with this name already exists");
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        ProjectMember creatorMember = ProjectMember.builder()
                .project(project)
                .user(createdBy)
                .accessLevel(AccessLevel.ADMIN)
                .assignedBy(createdBy)
                .build();
        project.getMembers().add(creatorMember);

        project = projectRepository.save(project);

        log.info("Project created: {} by {}", project.getName(), createdBy.getEmail());

        return toProjectResponse(project, createdBy);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId, User requestedBy) {
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canRead(projectId, requestedBy)) {
            throw new ForbiddenException("You don't have access to this project");
        }

        return toProjectResponse(project, requestedBy);
    }

    @Transactional(readOnly = true)
    public Page<ProjectSummaryResponse> listProjects(User requestedBy, boolean includeArchived, Pageable pageable) {
        Page<Project> projects;

        if (requestedBy.getRole() == LLMOPsRole.ADMIN) {
            if (includeArchived) {
                projects = projectRepository.findAll(pageable);
            } else {
                projects = projectRepository.findByStatus(ProjectStatus.ACTIVE, pageable);
            }
        } else {
            if (includeArchived) {
                projects = projectRepository.findAllAccessibleByUser(requestedBy.getId(), pageable);
            } else {
                projects = projectRepository.findAccessibleByUser(requestedBy.getId(), ProjectStatus.ACTIVE, pageable);
            }
        }

        return projects.map(p -> toProjectSummaryResponse(p, requestedBy));
    }

    @Transactional(readOnly = true)
    public Page<ProjectSummaryResponse> searchProjects(String search, User requestedBy, Pageable pageable) {
        return projectRepository.searchByNameOrDescription(search, ProjectStatus.ACTIVE, pageable)
                .map(p -> toProjectSummaryResponse(p, requestedBy));
    }

    @Transactional
    public ProjectResponse updateProject(UUID projectId, UpdateProjectRequest request, User updatedBy) {
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canAdmin(projectId, updatedBy)) {
            throw new ForbiddenException("You don't have permission to update this project");
        }

        if (request.getName() != null && !request.getName().equals(project.getName())) {
            if (projectRepository.existsByName(request.getName())) {
                throw new ConflictException("Project with this name already exists");
            }
            project.setName(request.getName());
        }

        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        project = projectRepository.save(project);

        log.info("Project updated: {} by {}", project.getName(), updatedBy.getEmail());

        return toProjectResponse(project, updatedBy);
    }

    @Transactional
    public void archiveProject(UUID projectId, User archivedBy) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canAdmin(projectId, archivedBy)) {
            throw new ForbiddenException("You don't have permission to archive this project");
        }

        project.archive();
        projectRepository.save(project);

        log.info("Project archived: {} by {}", project.getName(), archivedBy.getEmail());
    }

    @Transactional
    public ProjectResponse activateProject(UUID projectId, User activatedBy) {
        Project project = projectRepository.findByIdWithDetails(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canAdmin(projectId, activatedBy)) {
            throw new ForbiddenException("You don't have permission to activate this project");
        }

        project.activate();
        project = projectRepository.save(project);

        log.info("Project activated: {} by {}", project.getName(), activatedBy.getEmail());

        return toProjectResponse(project, activatedBy);
    }

    // Team Assignment

    @Transactional
    public ProjectTeamResponse assignTeam(UUID projectId, AssignTeamRequest request, User assignedBy) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canAdmin(projectId, assignedBy)) {
            throw new ForbiddenException("You don't have permission to assign teams to this project");
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        if (projectTeamRepository.existsByProjectIdAndTeamId(projectId, request.getTeamId())) {
            throw new ConflictException("Team is already assigned to this project");
        }

        ProjectTeam projectTeam = ProjectTeam.builder()
                .project(project)
                .team(team)
                .accessLevel(request.getAccessLevel())
                .assignedBy(assignedBy)
                .build();

        projectTeam = projectTeamRepository.save(projectTeam);

        log.info("Team {} assigned to project {} with {} access by {}",
                team.getName(), project.getName(), request.getAccessLevel(), assignedBy.getEmail());

        return toProjectTeamResponse(projectTeam);
    }

    @Transactional
    public ProjectTeamResponse updateTeamAccess(UUID projectId, UUID teamId, UpdateAccessLevelRequest request, User updatedBy) {
        if (!accessService.canAdmin(projectId, updatedBy)) {
            throw new ForbiddenException("You don't have permission to update team access");
        }

        ProjectTeam projectTeam = projectTeamRepository.findByProjectIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team is not assigned to this project"));

        projectTeam.setAccessLevel(request.getAccessLevel());
        projectTeam = projectTeamRepository.save(projectTeam);

        log.info("Team {} access updated to {} on project {} by {}",
                projectTeam.getTeam().getName(), request.getAccessLevel(),
                projectTeam.getProject().getName(), updatedBy.getEmail());

        return toProjectTeamResponse(projectTeam);
    }

    @Transactional
    public void removeTeam(UUID projectId, UUID teamId, User removedBy) {
        if (!accessService.canAdmin(projectId, removedBy)) {
            throw new ForbiddenException("You don't have permission to remove teams from this project");
        }

        ProjectTeam projectTeam = projectTeamRepository.findByProjectIdAndTeamId(projectId, teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team is not assigned to this project"));

        projectTeamRepository.delete(projectTeam);

        log.info("Team {} removed from project {} by {}",
                projectTeam.getTeam().getName(), projectTeam.getProject().getName(), removedBy.getEmail());
    }

    @Transactional(readOnly = true)
    public List<ProjectTeamResponse> getProjectTeams(UUID projectId, User requestedBy) {
        if (!accessService.canRead(projectId, requestedBy)) {
            throw new ForbiddenException("You don't have access to this project");
        }

        return projectTeamRepository.findByProjectId(projectId).stream()
                .map(this::toProjectTeamResponse)
                .toList();
    }

    // Member Assignment

    @Transactional
    public ProjectMemberResponse assignMember(UUID projectId, AssignMemberRequest request, User assignedBy) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException("Project not found"));

        if (!accessService.canAdmin(projectId, assignedBy)) {
            throw new ForbiddenException("You don't have permission to assign members to this project");
        }

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId())) {
            throw new ConflictException("User is already a member of this project");
        }

        ProjectMember member = ProjectMember.builder()
                .project(project)
                .user(user)
                .accessLevel(request.getAccessLevel())
                .assignedBy(assignedBy)
                .build();

        member = projectMemberRepository.save(member);

        log.info("User {} assigned to project {} with {} access by {}",
                user.getEmail(), project.getName(), request.getAccessLevel(), assignedBy.getEmail());

        return toProjectMemberResponse(member);
    }

    @Transactional
    public ProjectMemberResponse updateMemberAccess(UUID projectId, UUID userId, UpdateAccessLevelRequest request, User updatedBy) {
        if (!accessService.canAdmin(projectId, updatedBy)) {
            throw new ForbiddenException("You don't have permission to update member access");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new UserNotFoundException("User is not a member of this project"));

        // Cannot demote the last admin
        if (member.getAccessLevel() == AccessLevel.ADMIN &&
            request.getAccessLevel() != AccessLevel.ADMIN &&
            projectMemberRepository.countAdminsByProjectId(projectId) <= 1) {
            throw new ForbiddenException("Cannot demote the last project admin");
        }

        member.setAccessLevel(request.getAccessLevel());
        member = projectMemberRepository.save(member);

        log.info("User {} access updated to {} on project {} by {}",
                member.getUser().getEmail(), request.getAccessLevel(),
                member.getProject().getName(), updatedBy.getEmail());

        return toProjectMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID projectId, UUID userId, User removedBy) {
        if (!accessService.canAdmin(projectId, removedBy)) {
            throw new ForbiddenException("You don't have permission to remove members from this project");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new UserNotFoundException("User is not a member of this project"));

        // Cannot remove the last admin
        if (member.getAccessLevel() == AccessLevel.ADMIN &&
            projectMemberRepository.countAdminsByProjectId(projectId) <= 1) {
            throw new ForbiddenException("Cannot remove the last project admin");
        }

        projectMemberRepository.delete(member);

        log.info("User {} removed from project {} by {}",
                member.getUser().getEmail(), member.getProject().getName(), removedBy.getEmail());
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectMembers(UUID projectId, User requestedBy) {
        if (!accessService.canRead(projectId, requestedBy)) {
            throw new ForbiddenException("You don't have access to this project");
        }

        return projectMemberRepository.findByProjectId(projectId).stream()
                .map(this::toProjectMemberResponse)
                .toList();
    }

    private ProjectResponse toProjectResponse(Project project, User requestedBy) {
        String accessLevel = accessService.getUserAccessLevel(project.getId(), requestedBy)
                .map(Enum::name)
                .orElse(null);

        ProjectResponse.ProjectResponseBuilder builder = ProjectResponse.builder()
                .id(project.getId().toString())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .myAccessLevel(accessLevel);

        if (project.getCreatedBy() != null) {
            builder.createdById(project.getCreatedBy().getId().toString())
                    .createdByName(project.getCreatedBy().getFullName());
        }

        if (project.getTeams() != null) {
            builder.teams(project.getTeams().stream()
                    .map(this::toProjectTeamResponse)
                    .toList());
        }

        if (project.getMembers() != null) {
            builder.members(project.getMembers().stream()
                    .map(this::toProjectMemberResponse)
                    .toList());
        }

        return builder.build();
    }

    private ProjectSummaryResponse toProjectSummaryResponse(Project project, User requestedBy) {
        String accessLevel = accessService.getUserAccessLevel(project.getId(), requestedBy)
                .map(Enum::name)
                .orElse(null);

        int teamCount = 0;
        int memberCount = 0;
        try {
            teamCount = project.getTeams() != null ? project.getTeams().size() : 0;
            memberCount = project.getMembers() != null ? project.getMembers().size() : 0;
        } catch (Exception e) {
            // Collections not initialized, use 0
        }

        return ProjectSummaryResponse.builder()
                .id(project.getId().toString())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus().name())
                .teamCount(teamCount)
                .memberCount(memberCount)
                .createdAt(project.getCreatedAt())
                .myAccessLevel(accessLevel)
                .build();
    }

    private ProjectTeamResponse toProjectTeamResponse(ProjectTeam projectTeam) {
        ProjectTeamResponse.ProjectTeamResponseBuilder builder = ProjectTeamResponse.builder()
                .id(projectTeam.getId().toString())
                .teamId(projectTeam.getTeam().getId().toString())
                .teamName(projectTeam.getTeam().getName())
                .accessLevel(projectTeam.getAccessLevel().name())
                .assignedAt(projectTeam.getCreatedAt());

        if (projectTeam.getAssignedBy() != null) {
            builder.assignedById(projectTeam.getAssignedBy().getId().toString())
                    .assignedByName(projectTeam.getAssignedBy().getFullName());
        }

        return builder.build();
    }

    private ProjectMemberResponse toProjectMemberResponse(ProjectMember member) {
        ProjectMemberResponse.ProjectMemberResponseBuilder builder = ProjectMemberResponse.builder()
                .id(member.getId().toString())
                .userId(member.getUser().getId().toString())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .accessLevel(member.getAccessLevel().name())
                .assignedAt(member.getCreatedAt());

        if (member.getAssignedBy() != null) {
            builder.assignedById(member.getAssignedBy().getId().toString())
                    .assignedByName(member.getAssignedBy().getFullName());
        }

        return builder.build();
    }
}
