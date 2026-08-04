package com.javaweb.auth.controller;

import com.javaweb.auth.dto.AuthUserResponse;
import com.javaweb.auth.dto.ChangePasswordRequest;
import com.javaweb.auth.dto.LoginRequest;
import com.javaweb.auth.dto.LoginResponse;
import com.javaweb.auth.dto.LogoutRequest;
import com.javaweb.auth.dto.ProfileUpdateRequest;
import com.javaweb.auth.dto.RefreshTokenRequest;
import com.javaweb.auth.dto.RegisterRequest;
import com.javaweb.auth.dto.RegisterResponse;
import com.javaweb.auth.dto.SessionResponse;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.service.AuthService;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(security = {})
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("Registration successful", authService.register(request));
    }

    @Operation(security = {})
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @Operation(security = {})
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ApiResponse.success(
                "Token refreshed successfully",
                authService.refresh(request.refreshToken())
        );
    }

    @Operation(security = {})
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success("Logout successful", null);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current account")
    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> currentUser(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(authService.currentUser(principal));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current account profile")
    @PatchMapping("/me/profile")
    public ApiResponse<AuthUserResponse> updateProfile(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request
    ) {
        return ApiResponse.success(
                "Profile updated successfully",
                authService.updateProfile(principal, request)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Change current account password")
    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(principal, request);
        return ApiResponse.success("Password changed successfully", null);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload current account avatar")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AuthUserResponse> uploadAvatar(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.success(
                "Avatar uploaded successfully",
                authService.uploadAvatar(principal, file)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete current account avatar")
    @DeleteMapping("/me/avatar")
    public ApiResponse<AuthUserResponse> deleteAvatar(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(
                "Avatar deleted successfully",
                authService.deleteAvatar(principal)
        );
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List current account sessions")
    @GetMapping("/me/sessions")
    public ApiResponse<List<SessionResponse>> sessions(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(authService.sessions(principal));
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke one current account session")
    @DeleteMapping("/me/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSession(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long sessionId
    ) {
        authService.revokeSession(principal, sessionId);
    }

    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Revoke all current account sessions")
    @DeleteMapping("/me/sessions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSessions(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        authService.revokeSessions(principal);
    }
}
