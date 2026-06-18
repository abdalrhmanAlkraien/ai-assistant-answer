package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:38 AM
 */
@Builder
@Getter
public class FailureDto {

    private Long id;
    private String lang;
    private String searchType;
    private String metric;
    private BigDecimal score;
    private BigDecimal threshold;
    private String humanReadable;
}
