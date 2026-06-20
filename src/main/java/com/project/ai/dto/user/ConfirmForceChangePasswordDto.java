package com.project.ai.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 9:15 PM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmForceChangePasswordDto {

    @NotBlank
    private String email;
    @NotBlank private String session;       // from login response challenge
    @NotBlank private String newPassword;
}
