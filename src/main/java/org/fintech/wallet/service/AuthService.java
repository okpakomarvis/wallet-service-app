package org.fintech.wallet.service;

import org.fintech.wallet.dto.request.*;
import org.fintech.wallet.dto.response.AuthResponse;
import org.fintech.wallet.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    AuthResponse register(RegisterRequest request, String userAgent);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse refreshToken(String refreshToken, String userAgent);
    void changePassword(UUID userId, ChangePasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void setTransactionPin(UUID userId, String pin);
    void enableMfa(UUID userId, String secret);
    boolean verifyTransactionPin(UUID userId, String pin);
    void disableMfa(UUID userId);
    UserResponse getCurrentUser(UUID userId);
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request);
    void logout(UUID userId, String accessToken);
    void logoutAllDevices(UUID userId);

}
