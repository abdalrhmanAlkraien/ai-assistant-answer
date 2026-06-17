package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 11:45 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserHistorySummary {

    private String userId;
    private Long messageCount;
    private LocalDateTime lastActivity;
}
