package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 29/05/2026
 * @Time: 12:15 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecentRequestDto {
    private String userId;
    private String question;
    private String type;
    private String language;
    private Long responseTimeMs;
    private LocalDateTime createdAt;
}
