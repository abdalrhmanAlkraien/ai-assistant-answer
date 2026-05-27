package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 2:15 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromptStatsDto {

    private Long total;
    private Long active;
    private Long inactive;
}
