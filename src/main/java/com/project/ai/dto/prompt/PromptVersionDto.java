package com.project.ai.dto.prompt;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 13/06/2026
 * @Time: 3:57 PM
 */
@Data
@Builder
public class PromptVersionDto {
    private Long    id;
    private String  promptKey;
    private Integer version;
    private boolean isActive;
    private String  description;
    private String  updatedBy;
    private String  changeReason;
    private BigDecimal evalScore;
    private LocalDateTime evalRunAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;
}
