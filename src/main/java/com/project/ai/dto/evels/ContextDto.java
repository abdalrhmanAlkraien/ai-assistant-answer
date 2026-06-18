package com.project.ai.dto.evels;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:39 AM
 */
@Builder
@Getter
public class ContextDto {

    private Long id;
    private String question;
    private String language;
    private String searchType;
    private String expectedSearchType;
    private String returnedSearchType;
    private Boolean correctType;
    private Integer latencyMs;
    private String status;
    private Boolean passed;
    private String answer;
    private String groundTruth;
    private List<String> contexts;
    private List<String> matchedProductIds;
    private Integer productCount;
    private Integer expectedProductCount;
    private Integer llmJudge;
    private Boolean noError;
}
