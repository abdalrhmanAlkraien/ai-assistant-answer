package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 1:22 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PromptSummaryDto {
    private Long id;
    private String businessName;
    private String promptKey;
    private Integer version;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
