package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:38 AM
 */
@Builder
@Getter
public class EvaluationTypeDto {

    private Long id;
    private String lang;
    private String searchType;
    private String resultType;
    private Boolean passed;
    private BigDecimal passRate;
    private Integer passedCases;
    private Integer totalCases;
    private BigDecimal correctTypeRate;
    private BigDecimal rightProductsReturned;
    private BigDecimal noMissingProducts;
    private BigDecimal noHallucination;
    private BigDecimal clarificationPassRate;
    private BigDecimal safeResponseRate;
    private List<ContextDto> contexts;
}
