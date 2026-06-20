package com.project.ai.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:45 PM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
    @NotBlank        private String confirmationCode;
    @NotBlank        private String newPassword;
}
