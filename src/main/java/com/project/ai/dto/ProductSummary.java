package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 7:55 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductSummary {

    private String id;
    private String name;
    private String price;
    private String category;
    private String brand;
    private String description;
    private String imageUrl;
}
