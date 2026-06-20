package com.project.ai.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:42 PM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    // ── Tokens ────────────────────────────────────────────────────────────────
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;

    // ── Challenge ─────────────────────────────────────────────────────────────
    private String challengeName;
    private String session;
    private String challengeEmail;

    // ── User info ─────────────────────────────────────────────────────────────
    private String sub;
    private String username;
    private String email;
    private String name;
    private String familyName;
    private String phoneNumber;
    private String status;
    private List<String> groups;
    private String packageType;
    private List<String> features;
}
