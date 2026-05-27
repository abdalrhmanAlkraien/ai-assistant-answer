package com.project.ai.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:57 PM
 */
public record ProductRequest(
        @NotBlank String productId,
        @NotBlank String title,
        String category,
        String brand,
        Double price,
        String currency,
        String description,
        String imageUrl
) {
}
