package io.github.ashwithpoojary98.llmops_eval.project.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.project.dto.*;
import io.github.ashwithpoojary98.llmops_eval.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'DEVELOPER')")
    public ResponseEntity<APIResponse<ProjectResponse>> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectResponse response = projectService.createProject(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<ProjectResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Project created successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<ProjectSummaryResponse>>> listProjects(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());

        Page<ProjectSummaryResponse> projects;
        if (search != null && !search.isBlank()) {
            projects = projectService.searchProjects(search, user, pageable);
        } else {
            projects = projectService.listProjects(user, includeArchived, pageable);
        }

        return ResponseEntity.ok(APIResponse.<Page<ProjectSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(projects)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<ProjectResponse>> getProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectResponse response = projectService.getProject(id, user);

        return ResponseEntity.ok(APIResponse.<ProjectResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<ProjectResponse>> updateProject(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectResponse response = projectService.updateProject(id, request, user);

        return ResponseEntity.ok(APIResponse.<ProjectResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Project updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> archiveProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        projectService.archiveProject(id, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Project archived successfully")
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<APIResponse<ProjectResponse>> activateProject(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectResponse response = projectService.activateProject(id, user);

        return ResponseEntity.ok(APIResponse.<ProjectResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Project activated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    // Team Assignment Endpoints

    @GetMapping("/{id}/teams")
    public ResponseEntity<APIResponse<List<ProjectTeamResponse>>> getProjectTeams(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        List<ProjectTeamResponse> teams = projectService.getProjectTeams(id, user);

        return ResponseEntity.ok(APIResponse.<List<ProjectTeamResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(teams)
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/{id}/teams")
    public ResponseEntity<APIResponse<ProjectTeamResponse>> assignTeam(
            @PathVariable UUID id,
            @Valid @RequestBody AssignTeamRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectTeamResponse response = projectService.assignTeam(id, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<ProjectTeamResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Team assigned successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @PatchMapping("/{id}/teams/{teamId}")
    public ResponseEntity<APIResponse<ProjectTeamResponse>> updateTeamAccess(
            @PathVariable UUID id,
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateAccessLevelRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectTeamResponse response = projectService.updateTeamAccess(id, teamId, request, user);

        return ResponseEntity.ok(APIResponse.<ProjectTeamResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Team access updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<APIResponse<Void>> removeTeam(
            @PathVariable UUID id,
            @PathVariable UUID teamId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        projectService.removeTeam(id, teamId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Team removed successfully")
                .timestamp(Instant.now())
                .build());
    }

    // Member Assignment Endpoints

    @GetMapping("/{id}/members")
    public ResponseEntity<APIResponse<List<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        List<ProjectMemberResponse> members = projectService.getProjectMembers(id, user);

        return ResponseEntity.ok(APIResponse.<List<ProjectMemberResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(members)
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<APIResponse<ProjectMemberResponse>> assignMember(
            @PathVariable UUID id,
            @Valid @RequestBody AssignMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectMemberResponse response = projectService.assignMember(id, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<ProjectMemberResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Member assigned successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<APIResponse<ProjectMemberResponse>> updateMemberAccess(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateAccessLevelRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        ProjectMemberResponse response = projectService.updateMemberAccess(id, userId, request, user);

        return ResponseEntity.ok(APIResponse.<ProjectMemberResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Member access updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<APIResponse<Void>> removeMember(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        projectService.removeMember(id, userId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Member removed successfully")
                .timestamp(Instant.now())
                .build());
    }
}
