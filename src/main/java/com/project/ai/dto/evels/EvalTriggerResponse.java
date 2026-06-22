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
public class EvalTriggerResponse {

    private Integer evalId;
    private String status;
    private List<String> lang;
    private List<String> searchType;
    private Integer totalCases;
}
