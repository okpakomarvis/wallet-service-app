package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.domain.entity.User;
import org.fintech.wallet.domain.enums.KycStatus;
import org.fintech.wallet.domain.enums.UserRole;
import org.fintech.wallet.domain.enums.UserStatus;
import org.fintech.wallet.dto.request.*;
import org.fintech.wallet.dto.response.AuthResponse;
import org.fintech.wallet.dto.response.UserResponse;
import org.fintech.wallet.exception.InvalidCredentialsException;
import org.fintech.wallet.exception.UserAlreadyExistsException;
import org.fintech.wallet.repository.UserRepository;
import org.fintech.wallet.security.JwtTokenProvider;
import org.fintech.wallet.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request,  String userAgent) {
        log.info("Registering new user: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered");
        }

        if (request.getPhoneNumber() != null && userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        // Validate password strength
        validatePasswordStrength(request.getPassword());

        // Create user
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ROLE_USER);

        User user = User.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .status(UserStatus.ACTIVE)
                .kycStatus(KycStatus.NOT_STARTED)
                .mfaEnabled(false)
                .roles(roles)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Auto login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        String sessionId = UUID.randomUUID().toString();

        String accessToken = jwtTokenProvider.generateAccessToken(authentication, sessionId, user.getId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, sessionId,user.getId());
        // store session
        tokenBlacklistService.storeUserSession(user.getId(), sessionId, userAgent);
        // Store refresh token in Redis (7 days)
        tokenBlacklistService.storeRefreshToken(user.getId(), sessionId, refreshToken, 7);


        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("Login attempt for user: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase(),
                            request.getPassword()
                    )
            );

            User user = userDetailsService.getUserByEmail(request.getEmail());

            // Update last login
            user.setLastLoginAt(LocalDateTime.now());
            user.setLastLoginIp(ipAddress);
            userRepository.save(user);

            //generate session id
            String sessionId = UUID.randomUUID().toString();
            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication, sessionId,user.getId());
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication, sessionId, user.getId());
            // store session
            tokenBlacklistService.storeUserSession(user.getId(), sessionId, userAgent);

            // Store refresh token in Redis (7 days)

            tokenBlacklistService.storeRefreshToken(user.getId(), sessionId, refreshToken, 7);


            log.info("User logged in successfully: {}", user.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Login failed for user: {}", request.getEmail(), e);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional
    public void logout(UUID userId, String accessToken) {
        log.info("Logout requested for user: {}", userId);

        String sessionId = jwtTokenProvider.getSessionIdFromToken(accessToken);

        // 1. Blacklist access token
        long remainingTime = jwtTokenProvider.getTokenExpirationTime(accessToken);
        if (remainingTime > 0) {
            tokenBlacklistService.blacklistToken(accessToken, remainingTime);
        }

        // Invalidate refresh token for THIS session
        tokenBlacklistService.invalidateRefreshToken(userId, sessionId);

        // Remove user session
        tokenBlacklistService.removeUserSession(userId, sessionId);
        log.info("User logged out successfully: {}", userId);
    }

    @Override
    @Transactional
    public void logoutAllDevices(UUID userId) {
        log.info("Logout from all devices requested for user: {}", userId);
        // Remove all sessions
        tokenBlacklistService.logoutAllDevices(userId);

        // Remove all refresh tokens
        tokenBlacklistService.invalidateAllRefreshTokens(userId);

        // Update lastLogoutAt to invalidate all access tokens
        tokenBlacklistService.updateLastLogoutAt(userId);
        log.info("User logged out from all devices: {}", userId);
    }


    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken, String userAgent) {
        log.info("Refreshing token");

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userDetailsService.getUserByEmail(email);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null,
                user.getRoles().stream()
                        .map(role -> new org.springframework.security.core.authority.SimpleGrantedAuthority(role.name()))
                        .collect(java.util.stream.Collectors.toList())
        );
        //generate session id
        String sessionId = UUID.randomUUID().toString();
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication,sessionId,user.getId());
        // store session
        tokenBlacklistService.storeUserSession(user.getId(), sessionId, userAgent);
        // Store refresh token in Redis (7 days)
        tokenBlacklistService.storeRefreshToken(user.getId(), sessionId, refreshToken, 7);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(mapToUserResponse(user))
                .build();
    }
    @Override
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        // Validate new password
        validatePasswordStrength(request.getNewPassword());

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset requested for: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // phase 2 In production Send reset link to email
        // For now, we'll generate a temporary password
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);

        // Send email with temporary password
        log.info("Password reset completed for: {}", request.getEmail());
    }

    @Transactional
    public void setTransactionPin(UUID userId, String pin) {
        log.info("Setting transaction PIN for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate PIN (must be 4 digits)
        if (!pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN must be 4 digits");
        }

        user.setTransactionPin(passwordEncoder.encode(pin));
        userRepository.save(user);

        log.info("Transaction PIN set for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyTransactionPin(UUID userId, String pin) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTransactionPin() == null) {
            throw new RuntimeException("Transaction PIN not set");
        }

        return passwordEncoder.matches(pin, user.getTransactionPin());
    }

    @Override
    @Transactional
    public void enableMfa(UUID userId, String secret) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMfaEnabled(true);
        user.setMfaSecret(secret);
        userRepository.save(user);

        log.info("MFA enabled for user: {}", userId);
    }

    @Override
    @Transactional
    public void disableMfa(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);

        log.info("MFA disabled for user: {}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToUserResponse(user);
    }
    @Override
    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new UserAlreadyExistsException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);

        return mapToUserResponse(user);
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 12) + "Aa1!";
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .kycStatus(user.getKycStatus())
                .mfaEnabled(user.getMfaEnabled())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}

