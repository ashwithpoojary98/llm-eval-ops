package io.github.ashwithpoojary98.llmops_eval.team.service;

import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ConflictException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.UserNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import io.github.ashwithpoojary98.llmops_eval.team.dto.*;
import io.github.ashwithpoojary98.llmops_eval.team.entity.Team;
import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMember;
import io.github.ashwithpoojary98.llmops_eval.team.entity.TeamMemberRole;
import io.github.ashwithpoojary98.llmops_eval.team.exception.TeamMemberNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.team.exception.TeamNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.team.repository.TeamMemberRepository;
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
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, User createdBy) {
        if (teamRepository.existsByName(request.getName())) {
            throw new ConflictException("Team with this name already exists");
        }

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        // Add creator as team lead
        team.addMember(createdBy, TeamMemberRole.OWNER, createdBy);

        team = teamRepository.save(team);

        log.info("Team created: {} by {}", team.getName(), createdBy.getEmail());

        return toTeamResponse(team, true);
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(UUID teamId, User requestedBy) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check access - must be admin or team member
        if (!isAdminOrTeamMember(requestedBy, team)) {
            throw new ForbiddenException("You don't have access to this team");
        }

        return toTeamResponse(team, true);
    }

    @Transactional(readOnly = true)
    public Page<TeamSummaryResponse> listTeams(User requestedBy, Pageable pageable) {
        // Admins can see all teams, others see only their teams
        if (requestedBy.getRole() == LLMOPsRole.ADMIN) {
            return teamRepository.findAll(pageable)
                    .map(this::toTeamSummaryResponse);
        } else {
            return teamRepository.findByMemberUserId(requestedBy.getId(), pageable)
                    .map(this::toTeamSummaryResponse);
        }
    }

    @Transactional(readOnly = true)
    public Page<TeamSummaryResponse> searchTeams(String search, Pageable pageable) {
        return teamRepository.searchByNameOrDescription(search, pageable)
                .map(this::toTeamSummaryResponse);
    }

    @Transactional
    public TeamResponse updateTeam(UUID teamId, UpdateTeamRequest request, User updatedBy) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check permission - must be admin or team lead
        if (!canManageTeam(updatedBy, team)) {
            throw new ForbiddenException("You don't have permission to update this team");
        }

        // Check name uniqueness if changing
        if (request.getName() != null && !request.getName().equals(team.getName())) {
            if (teamRepository.existsByName(request.getName())) {
                throw new ConflictException("Team with this name already exists");
            }
            team.setName(request.getName());
        }

        if (request.getDescription() != null) {
            team.setDescription(request.getDescription());
        }

        team = teamRepository.save(team);

        log.info("Team updated: {} by {}", team.getName(), updatedBy.getEmail());

        return toTeamResponse(team, true);
    }

    @Transactional
    public void deleteTeam(UUID teamId, User deletedBy) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Only admins can delete teams
        if (deletedBy.getRole() != LLMOPsRole.ADMIN) {
            throw new ForbiddenException("Only admins can delete teams");
        }

        teamRepository.delete(team);

        log.info("Team deleted: {} by {}", team.getName(), deletedBy.getEmail());
    }

    @Transactional
    public TeamMemberResponse addMember(UUID teamId, AddTeamMemberRequest request, User addedBy) {
        Team team = teamRepository.findByIdWithMembers(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check permission
        if (!canManageTeam(addedBy, team)) {
            throw new ForbiddenException("You don't have permission to add members to this team");
        }

        // Check if user exists
        User userToAdd = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Check if already a member
        if (teamMemberRepository.existsByTeamIdAndUserId(teamId, request.getUserId())) {
            throw new ConflictException("User is already a member of this team");
        }

        TeamMember member = TeamMember.builder()
                .team(team)
                .user(userToAdd)
                .role(request.getRole())
                .addedBy(addedBy)
                .build();

        member = teamMemberRepository.save(member);

        log.info("Member added to team {}: {} as {} by {}",
                team.getName(), userToAdd.getEmail(), request.getRole(), addedBy.getEmail());

        return toTeamMemberResponse(member);
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(UUID teamId, UUID userId, UpdateTeamMemberRequest request, User updatedBy) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check permission
        if (!canManageTeam(updatedBy, team)) {
            throw new ForbiddenException("You don't have permission to update member roles");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

        // Cannot demote the last lead
        if (member.getRole() == TeamMemberRole.OWNER &&
            request.getRole() == TeamMemberRole.MEMBER &&
            teamMemberRepository.countLeadsByTeamId(teamId) <= 1) {
            throw new ForbiddenException("Cannot demote the last team lead");
        }

        member.setRole(request.getRole());
        member = teamMemberRepository.save(member);

        log.info("Member role updated in team {}: {} to {} by {}",
                team.getName(), member.getUser().getEmail(), request.getRole(), updatedBy.getEmail());

        return toTeamMemberResponse(member);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID userId, User removedBy) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check permission
        if (!canManageTeam(removedBy, team)) {
            throw new ForbiddenException("You don't have permission to remove members from this team");
        }

        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new TeamMemberNotFoundException("Team member not found"));

        // Cannot remove the last lead
        if (member.getRole() == TeamMemberRole.OWNER &&
            teamMemberRepository.countLeadsByTeamId(teamId) <= 1) {
            throw new ForbiddenException("Cannot remove the last team lead");
        }

        teamMemberRepository.delete(member);

        log.info("Member removed from team {}: {} by {}",
                team.getName(), member.getUser().getEmail(), removedBy.getEmail());
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId, User requestedBy) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new TeamNotFoundException("Team not found"));

        // Check access
        if (!isAdminOrTeamMember(requestedBy, team)) {
            throw new ForbiddenException("You don't have access to this team");
        }

        return teamMemberRepository.findByTeamId(teamId).stream()
                .map(this::toTeamMemberResponse)
                .toList();
    }

    private boolean isAdminOrTeamMember(User user, Team team) {
        return user.getRole() == LLMOPsRole.ADMIN || team.hasMember(user.getId());
    }

    private boolean canManageTeam(User user, Team team) {
        if (user.getRole() == LLMOPsRole.ADMIN) {
            return true;
        }

        return teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .map(TeamMember::isLead)
                .orElse(false);
    }

    private TeamResponse toTeamResponse(Team team, boolean includeMembers) {
        TeamResponse.TeamResponseBuilder builder = TeamResponse.builder()
                .id(team.getId().toString())
                .name(team.getName())
                .description(team.getDescription())
                .memberCount(team.getMemberCount())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt());

        if (team.getCreatedBy() != null) {
            builder.createdById(team.getCreatedBy().getId().toString())
                    .createdByName(team.getCreatedBy().getFullName());
        }

        if (includeMembers && team.getMembers() != null) {
            builder.members(team.getMembers().stream()
                    .map(this::toTeamMemberResponse)
                    .toList());
        }

        return builder.build();
    }

    private TeamSummaryResponse toTeamSummaryResponse(Team team) {
        return TeamSummaryResponse.builder()
                .id(team.getId().toString())
                .name(team.getName())
                .description(team.getDescription())
                .memberCount(team.getMemberCount())
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamMemberResponse toTeamMemberResponse(TeamMember member) {
        TeamMemberResponse.TeamMemberResponseBuilder builder = TeamMemberResponse.builder()
                .id(member.getId().toString())
                .userId(member.getUser().getId().toString())
                .email(member.getUser().getEmail())
                .fullName(member.getUser().getFullName())
                .role(member.getRole().name())
                .joinedAt(member.getCreatedAt());

        if (member.getAddedBy() != null) {
            builder.addedById(member.getAddedBy().getId().toString())
                    .addedByName(member.getAddedBy().getFullName());
        }

        return builder.build();
    }
}
