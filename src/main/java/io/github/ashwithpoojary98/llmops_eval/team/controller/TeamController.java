package io.github.ashwithpoojary98.llmops_eval.team.controller;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.model.APIResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.security.AuthenticatedUser;
import io.github.ashwithpoojary98.llmops_eval.auth.service.UserService;
import io.github.ashwithpoojary98.llmops_eval.team.dto.*;
import io.github.ashwithpoojary98.llmops_eval.team.service.TeamService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'DEVELOPER')")
    public ResponseEntity<APIResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TeamResponse response = teamService.createTeam(request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<TeamResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Team created successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @GetMapping
    public ResponseEntity<APIResponse<Page<TeamSummaryResponse>>> listTeams(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());

        Page<TeamSummaryResponse> teams;
        if (search != null && !search.isBlank()) {
            teams = teamService.searchTeams(search, pageable);
        } else {
            teams = teamService.listTeams(user, pageable);
        }

        return ResponseEntity.ok(APIResponse.<Page<TeamSummaryResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(teams)
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<TeamResponse>> getTeam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TeamResponse response = teamService.getTeam(id, user);

        return ResponseEntity.ok(APIResponse.<TeamResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTeamRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TeamResponse response = teamService.updateTeam(id, request, user);

        return ResponseEntity.ok(APIResponse.<TeamResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Team updated successfully")
                .data(response)
                .timestamp(Instant.now())
                .build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<Void>> deleteTeam(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        teamService.deleteTeam(id, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Team deleted successfully")
                .timestamp(Instant.now())
                .build());
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<APIResponse<List<TeamMemberResponse>>> getTeamMembers(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        List<TeamMemberResponse> members = teamService.getTeamMembers(id, user);

        return ResponseEntity.ok(APIResponse.<List<TeamMemberResponse>>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .data(members)
                .timestamp(Instant.now())
                .build());
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<APIResponse<TeamMemberResponse>> addMember(
            @PathVariable UUID id,
            @Valid @RequestBody AddTeamMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TeamMemberResponse response = teamService.addMember(id, request, user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(APIResponse.<TeamMemberResponse>builder()
                        .success(true)
                        .statusCode(HttpStatus.CREATED.value())
                        .message("Member added successfully")
                        .data(response)
                        .timestamp(Instant.now())
                        .build());
    }

    @PatchMapping("/{id}/members/{userId}")
    public ResponseEntity<APIResponse<TeamMemberResponse>> updateMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTeamMemberRequest request,
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        User user = userService.findById(authenticatedUser.getUserIdAsUUID());
        TeamMemberResponse response = teamService.updateMemberRole(id, userId, request, user);

        return ResponseEntity.ok(APIResponse.<TeamMemberResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Member role updated successfully")
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
        teamService.removeMember(id, userId, user);

        return ResponseEntity.ok(APIResponse.<Void>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("Member removed successfully")
                .timestamp(Instant.now())
                .build());
    }
}
