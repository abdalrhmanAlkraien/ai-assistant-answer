package com.project.ai.controller;

import com.project.ai.dto.user.AdminResetPasswordDto;
import com.project.ai.dto.user.CreateUserRequest;
import com.project.ai.dto.user.UpdateUserGroupRequest;
import com.project.ai.dto.user.UpdateUserRequest;
import com.project.ai.dto.user.UserDto;
import com.project.ai.service.CognitoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 9:04 PM
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {


    private final CognitoService cognitoService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> createUser(
            @RequestBody @Valid CreateUserRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cognitoService.createUser(request, jwt));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsers(
            @RequestParam(required = false) String nextToken,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(cognitoService.getUsers(nextToken, limit));
    }

    @GetMapping("/{username}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> getUserDetails(
            @PathVariable String username) {
        return ResponseEntity.ok(cognitoService.getUserDetails(username));
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String username,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(cognitoService.updateUser(username, request));
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String username) {
        cognitoService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{username}/group")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<UserDto> changeUserGroup(
            @PathVariable String username,
            @RequestBody @Valid UpdateUserGroupRequest request) {
        return ResponseEntity.ok(cognitoService.changeUserGroup(username, request));
    }

    @PatchMapping("/{username}/reset-password")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> adminResetPassword(
            @PathVariable String username,
            @RequestBody @Valid AdminResetPasswordDto request) {
        cognitoService.adminResetPassword(username, request);
        return ResponseEntity.ok().build();
    }
}
