package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 08/05/2026
 * @Time: 12:56 AM
 */
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class SearchIntent {

    private String searchType;
    private Double minPrice;
    private Double maxPrice;
    private String category;
    private String brand;

    @Builder.Default
    private String semanticQuery = "";
}
