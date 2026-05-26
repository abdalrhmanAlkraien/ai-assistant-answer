package com.project.ai.model.planner;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:43 PM
 */
public record AnalysisJson(
        String enrichedQuestion,
        String complexity,
        List<String> intentTypes,
        boolean isMultiStep,
        boolean isAmbiguous,
        boolean requiresMemoryContext,
        boolean relatedToPreviousContext,   // ← NEW
        String searchType,
        String category,
        String brand,
        Double minPrice,
        Double maxPrice,
        String sortDirection
) {
}
