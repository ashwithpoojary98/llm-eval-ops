package io.github.ashwithpoojary98.llmops_eval.auth.service;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.*;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.Organization;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.UserInvitation;
import io.github.ashwithpoojary98.llmops_eval.auth.event.InvitationAcceptedEvent;
import io.github.ashwithpoojary98.llmops_eval.auth.event.UserInvitedEvent;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ConflictException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.ForbiddenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.InvalidTokenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.UserNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.auth.model.LLMOPsRole;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserInvitationRepository;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationService {

    private static final int TOKEN_EXPIRY_HOURS = 24;

    private final UserInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final DomainValidationService domainValidationService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InviteUserResponse inviteUser(InviteUserRequest request, User invitedBy) {
        String email = request.getEmail().toLowerCase().trim();
        Organization org = invitedBy.getOrganization();

        // 1. Validate domain
        domainValidationService.validateEmailDomain(email, org);

        // 2. Check if user already exists
        if (userRepository.existsByEmailAndOrganization(email, org)) {
            throw new ConflictException("User already exists with this email");
        }

        // 3. Check for pending invitation
        invitationRepository.findPendingByEmailAndOrg(email, org.getId(), Instant.now())
                .ifPresent(existing -> {
                    throw new ConflictException("Pending invitation already exists for this email");
                });

        // 4. Validate role assignment
        validateRoleAssignment(request.getRole(), invitedBy.getRole());

        // 5. Generate secure token
        String token = generateSecureToken();
        String tokenHash = hashToken(token);

        // 6. Create invitation
        UserInvitation invitation = UserInvitation.builder()
                .email(email)
                .role(request.getRole())
                .organization(org)
                .invitedBy(invitedBy)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS))
                .build();

        invitation = invitationRepository.save(invitation);

        log.info("User invited: {} by {} with role {}",
                email, invitedBy.getEmail(), request.getRole());

        // Publish event for email notification
        eventPublisher.publishEvent(UserInvitedEvent.builder()
                .email(email)
                .invitedByName(invitedBy.getFullName())
                .organizationName(org.getName())
                .invitationToken(token)
                .role(request.getRole().name())
                .expiresAt(invitation.getExpiresAt())
                .build());

        // Return response with token (only returned once)
        return InviteUserResponse.builder()
                .invitationId(invitation.getId().toString())
                .email(invitation.getEmail())
                .role(invitation.getRole().name())
                .invitationToken(token)  // Only returned on creation
                .expiresAt(invitation.getExpiresAt())
                .invitedBy(invitedBy.getFullName())
                .organizationName(org.getName())
                .build();
    }

    @Transactional(readOnly = true)
    public InvitationDetailsResponse getInvitationDetails(String token) {
        String tokenHash = hashToken(token);

        UserInvitation invitation = invitationRepository
                .findValidByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation"));

        return InvitationDetailsResponse.builder()
                .email(invitation.getEmail())
                .role(invitation.getRole().name())
                .organizationName(invitation.getOrganization().getName())
                .invitedBy(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getFullName() : null)
                .expiresAt(invitation.getExpiresAt())
                .build();
    }

    @Transactional
    public AcceptInvitationResponse acceptInvitation(String token, AcceptInvitationRequest request) {
        String tokenHash = hashToken(token);

        UserInvitation invitation = invitationRepository
                .findValidByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired invitation"));

        // Validate password confirmation
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new io.github.ashwithpoojary98.llmops_eval.auth.exception.ValidationException(
                    "Passwords do not match"
            );
        }

        // Create user
        User user = User.builder()
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(invitation.getRole())
                .organization(invitation.getOrganization())
                .emailVerified(true)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        // Mark invitation as accepted
        invitation.setAcceptedAt(Instant.now());
        invitationRepository.save(invitation);

        log.info("Invitation accepted: {} joined as {}", user.getEmail(), user.getRole());

        // Publish event for welcome email
        eventPublisher.publishEvent(InvitationAcceptedEvent.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .organizationName(user.getOrganization().getName())
                .role(user.getRole().name())
                .build());

        return AcceptInvitationResponse.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .organizationId(user.getOrganization().getId().toString())
                .organizationName(user.getOrganization().getName())
                .build();
    }

    @Transactional(readOnly = true)
    public Page<InvitationSummaryResponse> listInvitationsByStatus(UUID organizationId, String status, Pageable pageable) {
        Instant now = Instant.now();
        return switch (status.toUpperCase()) {
            case "PENDING"  -> invitationRepository.findPendingByOrganization(organizationId, now, pageable).map(this::toSummaryResponse);
            case "EXPIRED"  -> invitationRepository.findExpiredByOrganization(organizationId, now, pageable).map(this::toSummaryResponse);
            case "ACCEPTED" -> invitationRepository.findAcceptedByOrganization(organizationId, pageable).map(this::toSummaryResponse);
            case "REVOKED"  -> invitationRepository.findRevokedByOrganization(organizationId, pageable).map(this::toSummaryResponse);
            default         -> invitationRepository.findAllByOrganization(organizationId, pageable).map(this::toSummaryResponse);
        };
    }

    @Transactional
    public void revokeInvitation(UUID invitationId, UUID revokedById) {
        User revoker = userRepository.findById(revokedById)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        UserInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new InvalidTokenException("Invitation not found"));

        // Verify same organization
        if (!invitation.getOrganization().getId().equals(revoker.getOrganization().getId())) {
            throw new ForbiddenException("Cannot revoke invitation from another organization");
        }

        int revoked = invitationRepository.revokeById(invitationId, Instant.now());
        if (revoked > 0) {
            log.info("Invitation revoked: {} by {}", invitation.getEmail(), revoker.getEmail());
        }
    }

    private void validateRoleAssignment(LLMOPsRole targetRole, LLMOPsRole inviterRole) {
        if (!inviterRole.canInvite(targetRole)) {
            throw new ForbiddenException(
                    "You don't have permission to invite users with role: " + targetRole
            );
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private InvitationSummaryResponse toSummaryResponse(UserInvitation invitation) {
        String status = "PENDING";
        if (invitation.isAccepted()) {
            status = "ACCEPTED";
        } else if (invitation.isRevoked()) {
            status = "REVOKED";
        } else if (invitation.isExpired()) {
            status = "EXPIRED";
        }

        return InvitationSummaryResponse.builder()
                .id(invitation.getId().toString())
                .email(invitation.getEmail())
                .role(invitation.getRole().name())
                .status(status)
                .invitedBy(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getFullName() : null)
                .createdAt(invitation.getCreatedAt())
                .expiresAt(invitation.getExpiresAt())
                .acceptedAt(invitation.getAcceptedAt())
                .build();
    }
}
