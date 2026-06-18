package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:38 AM
 */
@Builder
@Getter
public class EvaluationDetailDto {

    private Long id;
    private UUID runId;
    private String status;
    private String triggeredType;
    private String language;
    private Integer totalEvaluated;
    private Integer totalSkipped;
    private Boolean passed;
    private OffsetDateTime runAt;
    private OffsetDateTime completedAt;
    private List<EvaluationTypeDto> types;
    private List<FailureDto> failures;
    private String reportStatus;     // "pending" | "uploaded" | "failed"
    private String reportUrl;        // null if not uploaded yet
}
