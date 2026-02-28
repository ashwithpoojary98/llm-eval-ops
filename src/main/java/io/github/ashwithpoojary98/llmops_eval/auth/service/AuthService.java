package io.github.ashwithpoojary98.llmops_eval.auth.service;

import io.github.ashwithpoojary98.llmops_eval.auth.dto.LoginRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.LoginResponse;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.RefreshTokenRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.dto.UpdateUserPasswordRequest;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.RefreshToken;
import io.github.ashwithpoojary98.llmops_eval.auth.entity.User;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.AuthenticationException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.InvalidTokenException;
import io.github.ashwithpoojary98.llmops_eval.auth.exception.UserNotFoundException;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.RefreshTokenRepository;
import io.github.ashwithpoojary98.llmops_eval.auth.repository.UserRepository;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findActiveByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (Boolean.TRUE.equals(user.getIsSsoUser()) && user.getPasswordHash() == null) {
            throw new AuthenticationException("Please use SSO to login");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Failed login attempt for email: {}", request.getEmail());
            throw new AuthenticationException("Invalid email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthenticationException("Account is deactivated");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .ipAddress(ipAddress)
                .deviceInfo(userAgent)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        userRepository.updateLastLoginAt(user.getId(), Instant.now());

        log.info("User logged in successfully: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(1800) // 30 minutes in seconds
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .organizationId(user.getOrganization().getId().toString())
                .organizationName(user.getOrganization().getName())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    public LoginResponse refreshAccessToken(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.getRefreshToken());

        RefreshToken refreshToken = refreshTokenRepository
                .findValidByTokenHash(tokenHash, Instant.now())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired refresh token"));

        User user = refreshToken.getUser();

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AuthenticationException("Account is deactivated");
        }

        String accessToken = jwtService.generateAccessToken(user);

        String newRefreshToken = jwtService.generateRefreshToken();

        refreshToken.setRevokedAt(Instant.now());

        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(newRefreshToken))
                .expiresAt(jwtService.getRefreshTokenExpiry())
                .ipAddress(refreshToken.getIpAddress())
                .deviceInfo(refreshToken.getDeviceInfo())
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        log.info("Access token refreshed for user: {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(1800)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .organizationId(user.getOrganization().getId().toString())
                .organizationName(user.getOrganization().getName())
                .mustChangePassword(Boolean.TRUE.equals(user.getMustChangePassword()))
                .build();
    }

    @Transactional
    public void logout(UUID userId, @Nullable String refreshToken) {

        Instant now = Instant.now();

        if (!StringUtils.hasText(refreshToken)) {
            int revoked = refreshTokenRepository.revokeAllByUserId(userId, now);
            log.info("User logged out from all sessions userId={} revokedCount={}", userId, revoked);
            return;
        }

        String tokenHash = jwtService.hashToken(refreshToken);

        int revoked = refreshTokenRepository.revokeByUserIdAndTokenHash(
                userId,
                tokenHash,
                now
        );

        if (revoked > 0) {
            log.info("User logged out from single session userId={}", userId);
        } else {
            log.warn("Logout requested but refresh token not found userId={}", userId);
        }
    }


    @Transactional
    public void logoutAllSessions(UUID userId) {
        Instant now = Instant.now();
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, now);
        log.info(
                "All sessions revoked userId={} revokedCount={}",
                userId,
                revokedCount
        );
    }


    @Transactional
    public void updatePassword(String email, UpdateUserPasswordRequest request) {
        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("The current password you entered is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        user.setMustChangePassword(false);

        userRepository.save(user);
        log.info("Password updated successfully for user: {}", email);
    }
}
