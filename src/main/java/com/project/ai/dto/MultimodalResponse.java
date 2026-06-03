package com.project.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ai.agents.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:16 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MultimodalResponse {

    private String question;
    private String type;
    private String answer;
    private List<ProductSummary> products;
    @JsonIgnore
    private List<String> matchProducts;
    private Language language;
    private InputType inputType;
    private LocalDateTime responseTime;
    private List<String> suggestedOptions;
    private String transcribedText;
}
