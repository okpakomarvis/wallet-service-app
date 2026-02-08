package org.fintech.wallet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.fintech.wallet.dto.request.*;
import org.fintech.wallet.dto.response.ApiResponse;
import org.fintech.wallet.dto.response.AuthResponse;
import org.fintech.wallet.dto.response.UserResponse;
import org.fintech.wallet.security.CurrentUser;
import org.fintech.wallet.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication and user account APIs")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register user",
            description = "Create a new user account"
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.register(request, userAgent,ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    @Operation(
            summary = "Refresh access token",
            description = "Refresh access token using a valid refresh token"
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

        String userAgent = httpRequest.getHeader("User-Agent");
        AuthResponse response = authService.refreshToken(request.getRefreshToken(), userAgent);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @Operation(
            summary = "Login",
            description = "Authenticate user with credentials"
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @Parameter(hidden = true) HttpServletRequest httpRequest) {

        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        AuthResponse response = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(
            summary = "Logout",
            description = "Logout user from current session"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Parameter(hidden = true) HttpServletRequest request) {

        String token = getJwtFromRequest(request);

        if (token != null) {
            authService.logout(userId, token);
        }

        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @Operation(
            summary = "Logout from all devices",
            description = "Invalidate all active sessions for the user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout-all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> logoutAllDevices(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        authService.logoutAllDevices(userId);
        return ResponseEntity.ok(
                ApiResponse.success("Logged out from all devices successfully", null)
        );
    }

    @Operation(
            summary = "Get current user",
            description = "Retrieve details of the authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        UserResponse user = authService.getCurrentUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @Operation(
            summary = "Update profile",
            description = "Update authenticated user's profile"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {

        UserResponse user = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
    }

    @Operation(
            summary = "Change password",
            description = "Change account password for authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Passwords do not match"));
        }

        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Operation(
            summary = "Reset password",
            description = "Send password reset instructions to email"
    )
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Password reset instructions sent to email", null)
        );
    }

    @Operation(
            summary = "Set transaction PIN",
            description = "Set transaction PIN for authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/set-pin")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> setTransactionPin(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @Valid @RequestBody SetPinRequest request) {

        if (!request.getPin().equals(request.getConfirmPin())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("PINs do not match"));
        }

        authService.setTransactionPin(userId, request.getPin());
        return ResponseEntity.ok(
                ApiResponse.success("Transaction PIN set successfully", null)
        );
    }

    @Operation(
            summary = "Enable MFA",
            description = "Enable multi-factor authentication for user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/mfa/enable")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> enableMfa(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @RequestBody String secret) {

        authService.enableMfa(userId, secret);
        return ResponseEntity.ok(ApiResponse.success("MFA enabled", null));
    }

    @Operation(
            summary = "Disable MFA",
            description = "Disable multi-factor authentication for user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/mfa/disable")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<Void>> disableMfa(
            @Parameter(hidden = true) @CurrentUser UUID userId) {

        authService.disableMfa(userId);
        return ResponseEntity.ok(ApiResponse.success("MFA disabled", null));
    }
    @Operation(
            summary = "Upload profile picture",
            description = "Upload or replace profile picture for authenticated user"
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/profile/image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> uploadProfileImage(
            @Parameter(hidden = true) @CurrentUser UUID userId,
            @RequestPart("image") MultipartFile image
    ) {
        UserResponse user = authService.updateProfileImage(userId, image);
        return ResponseEntity.ok(
                ApiResponse.success("Profile image updated successfully", user)
        );
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}