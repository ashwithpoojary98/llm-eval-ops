package io.github.ashwithpoojary98.llmops_eval.auth.service;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.UpdateUserRoleRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.UserSummaryResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.event.UserDeactivatedEvent;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.UserNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.RefreshTokenRepository;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findActiveByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsersByOrganization(UUID organizationId, Pageable pageable) {
        return userRepository.findByOrganizationId(organizationId, pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional
    public UserSummaryResponse updateUserRole(UUID userId, UpdateUserRoleRequest request, User updatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify same organization
        if (!user.getOrganization().getId().equals(updatedBy.getOrganization().getId())) {
            throw new ForbiddenException("Cannot modify user from another organization");
        }

        // Cannot modify own role
        if (user.getId().equals(updatedBy.getId())) {
            throw new ForbiddenException("Cannot modify your own role");
        }

        // Validate role change permission
        if (!updatedBy.getRole().canInvite(request.getRole())) {
            throw new ForbiddenException("You don't have permission to assign this role");
        }

        LLMOPsRole oldRole = user.getRole();
        user.setRole(request.getRole());
        user = userRepository.save(user);

        log.info("User role updated: {} from {} to {} by {}",
                user.getEmail(), oldRole, request.getRole(), updatedBy.getEmail());

        return toSummaryResponse(user);
    }

    @Transactional
    public void deactivateUser(UUID userId, User deactivatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify same organization
        if (!user.getOrganization().getId().equals(deactivatedBy.getOrganization().getId())) {
            throw new ForbiddenException("Cannot deactivate user from another organization");
        }

        // Cannot deactivate self
        if (user.getId().equals(deactivatedBy.getId())) {
            throw new ForbiddenException("Cannot deactivate your own account");
        }

        // Cannot deactivate higher or equal role
        if (user.getRole().getPriority() >= deactivatedBy.getRole().getPriority()) {
            throw new ForbiddenException("Cannot deactivate user with equal or higher role");
        }

        user.setIsActive(false);
        userRepository.save(user);

        // Revoke all refresh tokens
        refreshTokenRepository.revokeAllByUserId(userId, Instant.now());

        log.info("User deactivated: {} by {}", user.getEmail(), deactivatedBy.getEmail());

        // Publish event for deactivation email
        eventPublisher.publishEvent(UserDeactivatedEvent.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .organizationName(user.getOrganization().getName())
                .build());
    }

    @Transactional
    public UserSummaryResponse reactivateUser(UUID userId, User reactivatedBy) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify same organization
        if (!user.getOrganization().getId().equals(reactivatedBy.getOrganization().getId())) {
            throw new ForbiddenException("Cannot reactivate user from another organization");
        }

        user.setIsActive(true);
        user = userRepository.save(user);

        log.info("User reactivated: {} by {}", user.getEmail(), reactivatedBy.getEmail());

        return toSummaryResponse(user);
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUserDetails(UUID userId, UUID requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new UserNotFoundException("Requester not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Verify same organization
        if (!user.getOrganization().getId().equals(requester.getOrganization().getId())) {
            throw new ForbiddenException("Cannot view user from another organization");
        }

        return toSummaryResponse(user);
    }

    private UserSummaryResponse toSummaryResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .isSsoUser(user.getIsSsoUser())
                .emailVerified(user.getEmailVerified())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
