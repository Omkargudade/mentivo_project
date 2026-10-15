package com.mentivo.controller;

import com.mentivo.dto.ApiResponse;
import com.mentivo.dto.auth.AuthResponse;
import com.mentivo.dto.auth.LoginRequest;
import com.mentivo.dto.auth.ModeSwitchRequest;
import com.mentivo.dto.auth.RefreshTokenRequest;
import com.mentivo.dto.auth.RegisterRequest;
import com.mentivo.security.SecurityUtils;
import com.mentivo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User registration, login, token refresh, and mode switching")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register new account (Learner, Mentor, or Both)")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response.getMessage() != null ? response.getMessage() : "Account created successfully", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset link via registered email")
    public ResponseEntity<ApiResponse<AuthResponse>> forgotPassword(@Valid @RequestBody com.mentivo.dto.auth.ForgotPasswordRequest request) {
        AuthResponse response = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using valid single-use reset token")
    public ResponseEntity<ApiResponse<AuthResponse>> resetPassword(@Valid @RequestBody com.mentivo.dto.auth.ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Logged in successfully", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh expired JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/switch-mode")
    @Operation(summary = "Switch active mode between LEARNER and MENTOR")
    public ResponseEntity<ApiResponse<AuthResponse>> switchMode(@Valid @RequestBody ModeSwitchRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AuthResponse response = authService.switchMode(currentUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Switched to " + request.getMode() + " mode", response));
    }
}
