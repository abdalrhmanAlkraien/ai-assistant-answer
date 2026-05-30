package com.project.ai.model.planner;

import com.project.ai.agents.Language;
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
public class ClarificationContext {

    private final String originalQuestion;
    private final String clarificationQuestion;  // in user's language
    private final List<String> suggestedOptions; // suggested choices
    private final Language language;
    private final boolean isGreeting;
}
