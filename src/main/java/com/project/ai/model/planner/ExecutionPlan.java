package com.project.ai.model.planner;

import com.project.ai.agents.Language;
import com.project.ai.dto.SearchIntent;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:33 PM
 */
@Builder
@Getter
public class ExecutionPlan {

    private final List<ExecutionStep> steps;
    private final ComplexityLevel overallComplexity;
    private final Language language;
    private final boolean requiresClarification;
    private final ClarificationContext clarificationContext; // null if no clarification needed
    private final SearchIntent normalizedIntent;
    private final String primaryModel;
}
