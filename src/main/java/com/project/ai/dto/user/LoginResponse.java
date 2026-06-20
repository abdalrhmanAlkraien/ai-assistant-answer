package com.project.ai.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private Integer expiresIn;
    private String tokenType;

    private String challengeName;   // "NEW_PASSWORD_REQUIRED"
    private String session;         // pass back to confirm endpoint
    private String challengeEmail;  // so frontend knows which user
}
