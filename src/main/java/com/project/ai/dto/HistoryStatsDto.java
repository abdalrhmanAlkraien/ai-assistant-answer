package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 12:24 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HistoryStatsDto {
    private Long totalUsers;
    private Long totalRecords;
}
