package com.project.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)  // ← ADD THIS
public class SearchIntent {

    private String searchType;
    private Double minPrice;
    private Double maxPrice;
    private String category;
    private String brand;
    private String sortDirection;  // ← add this

    @Builder.Default
    private String semanticQuery = "";
    private String semanticQueryArabic;      // ← ADD THIS

}
