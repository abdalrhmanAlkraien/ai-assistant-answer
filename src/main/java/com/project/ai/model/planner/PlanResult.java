package com.project.ai.model.planner;

import com.project.ai.agents.Language;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:34 PM
 */
@Builder
@Getter
public class PlanResult {

    private final String answer;
    private final String type;
    private final List<String> matchedIds;
    private final Language language;
    private final boolean clarificationRequired;
    private final ClarificationContext clarificationContext;
    private final int stepsExecuted;
    private final boolean fallbackUsed;
}
