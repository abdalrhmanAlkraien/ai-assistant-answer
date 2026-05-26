package com.project.ai.model.planner;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 24/05/2026
 * @Time: 12:38 AM
 */
public record PlannerMemoryResult(
        String memoryContext,    // raw memory — for planner context
        String enrichedQuestion  // enriched question — replaces original
) {}