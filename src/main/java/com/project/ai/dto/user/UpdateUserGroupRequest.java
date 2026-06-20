package com.project.ai.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 20/06/2026
 * @Time: 7:50 AM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserGroupRequest {
    @NotBlank
    private String group; // "MIGFORA_ADMIN" | "SUPER_ADMIN" | "BASIC"
}
