package com.project.ai.dto.evels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/06/2026
 * @Time: 7:18 AM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvalPollResponse {

    private Integer evalId;
    private String status;
    private Boolean passed;
    private Integer totalEvaluated;
    private Integer totalSkipped;
    private List<String> lang;
    private String resultType;
    private Boolean hasInconclusive;
    private String createdAt;
    private String completedAt;
    private String reportUrl;
    private Integer casesCompleted;
    private Integer casesTotal;
    private Double progressPct;
}
