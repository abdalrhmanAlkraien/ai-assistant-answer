package com.project.ai.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
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
public class CreateUserRequest {

    @NotBlank @Email
    private String email;
    @NotBlank        private String name;
    @NotBlank        private String familyName;
    @NotBlank        private String phoneNumber;
    @NotBlank
    private String password;
    private String group; // "MIGFORA_ADMIN" | "SUPER_ADMIN" | "BASIC"
}
