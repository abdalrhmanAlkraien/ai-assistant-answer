package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 11:49 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DashboardStatsDto {

    private Long requestsToday;
    private Long requestsTotal;
    private Long activeUsersToday;
    private Long totalProducts;
    private Long activePrompts;
    private Double avgResponseTimeMs;
    private Long tokensToday;
    private Long tokensTotal;
}
