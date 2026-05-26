package com.project.ai.model.planner;

import com.project.ai.agents.Language;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:35 PM
 */
@Builder
@Getter
public class RequestAnalysis {

    private final String enrichedQuestion;
    private final Language language;
    private final ComplexityLevel complexity;
    private final List<IntentType> intentTypes;
    private final boolean isMultiStep;
    private final boolean isAmbiguous;
    private final boolean requiresMemoryContext;
    private final String normalizedQuestion;  // cleaned, trimmed
    private final boolean relatedToPreviousContext;    // ← NEW

    // full intent fields ← NEW
    private final String searchType;
    private final String category;
    private final String brand;
    private final Double minPrice;
    private final Double maxPrice;
    private final String sortDirection;
}
