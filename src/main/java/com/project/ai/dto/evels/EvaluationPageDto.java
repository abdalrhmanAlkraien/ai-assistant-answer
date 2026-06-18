package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:38 AM
 */
@Builder
@Getter
public class EvaluationPageDto {

    private Long id;
    private UUID runId;
    private String status;
    private String triggeredType;   // "All" or "Specific"
    private String language;        // "Arabic", "English", "All"
    private Integer totalQueries;
    private OffsetDateTime runAt;
    private Boolean passed;          // ← add
}
