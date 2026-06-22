package com.project.ai.dto.evels;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 21/06/2026
 * @Time: 7:17 AM
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EvalTriggerRequest {

    @NotNull  private List<String> lang;
    @NotNull
    private List<String> searchType;
    private Integer limit;
    private Boolean saveReport = true;
}
