package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 29/05/2026
 * @Time: 12:12 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseTimeTrendDto {
    private String date;
    private Double avgResponseTimeMs;
}
