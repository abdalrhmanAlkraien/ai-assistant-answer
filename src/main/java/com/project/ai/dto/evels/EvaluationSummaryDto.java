package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:40 AM
 */
@Builder
@Getter
public class EvaluationSummaryDto {

    private OffsetDateTime lastRun;
    private String status;
    private String triggeredType;
    private String language;
    private Integer totalQueries;
    private Boolean passed;          // ← add
    private String reportStatus;     // ← add, useful for dashboard badge
    private String reportUrl;        // ← add
}
