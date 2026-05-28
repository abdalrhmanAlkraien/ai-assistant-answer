package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 29/05/2026
 * @Time: 12:08 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LanguageDistributionDto {
    private String language;
    private Long count;
}
