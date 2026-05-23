package com.project.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:07 PM
 */

public record CategoryRequest(
        @NotBlank String name,
        String slug,
        String nameArabic,
        String description
) {
}
