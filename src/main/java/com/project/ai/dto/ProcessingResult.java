package com.project.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:08 PM
 */
@Getter
@Builder
public class ProcessingResult {

    private final String enrichedQuestion;
    private final String type;
    private final String answer;
    private final List<String> matchedIds;
}
