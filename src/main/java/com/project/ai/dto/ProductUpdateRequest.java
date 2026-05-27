package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 8:05 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductUpdateRequest {

    private String productId;
    private String title;
    private String category;
    private String brand;
    private Double price;
    private String currency;
    private String description;
    private String imageUrl;
    private Boolean active;
}
