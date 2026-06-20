package com.project.ai.controller;

import com.project.ai.dto.user.ConfirmForceChangePasswordDto;
import com.project.ai.dto.user.ConfirmForgotPasswordRequest;
import com.project.ai.dto.user.ForgotPasswordRequest;
import com.project.ai.dto.user.LoginRequest;
import com.project.ai.dto.user.LoginResponse;
import com.project.ai.dto.user.RefreshTokenRequest;
import com.project.ai.dto.user.ResetPasswordRequest;
import com.project.ai.dto.user.UserDto;
import com.project.ai.service.CognitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 9:03 PM
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final CognitoService cognitoService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(cognitoService.login(request));
    }

    @PostMapping("/confirm-force-change-password")
    public ResponseEntity<LoginResponse> confirmForceChangePassword(
            @RequestBody @Valid ConfirmForceChangePasswordDto request) {
        return ResponseEntity.ok(cognitoService.confirmForceChangePassword(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(cognitoService.refreshToken(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request) {
        cognitoService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/confirm-forgot-password")
    public ResponseEntity<Void> confirmForgotPassword(
            @RequestBody @Valid ConfirmForgotPasswordRequest request) {
        cognitoService.confirmForgotPassword(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ResetPasswordRequest request) {
        String accessToken = authHeader.replace("Bearer ", "");
        cognitoService.resetPassword(accessToken, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        String accessToken = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(cognitoService.getCurrentUser(accessToken));
    }
}
