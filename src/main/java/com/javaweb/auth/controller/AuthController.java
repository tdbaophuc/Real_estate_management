package com.javaweb.auth.controller;

import com.javaweb.auth.dto.AuthUserResponse;
import com.javaweb.auth.dto.LoginRequest;
import com.javaweb.auth.dto.LoginResponse;
import com.javaweb.auth.security.AuthUserPrincipal;
import com.javaweb.auth.service.AuthService;
import com.javaweb.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(security = {})
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("Login successful", authService.login(request));
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<AuthUserResponse> currentUser(
            @AuthenticationPrincipal AuthUserPrincipal principal
    ) {
        return ApiResponse.success(AuthUserResponse.from(principal));
    }
}
