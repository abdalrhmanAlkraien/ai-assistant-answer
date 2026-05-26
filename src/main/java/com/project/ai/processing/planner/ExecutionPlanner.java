package com.project.ai.processing.planner;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.model.planner.ExecutionPlan;
import com.project.ai.model.planner.RequestAnalysis;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 12:21 AM
 */
public interface ExecutionPlanner {

    ExecutionPlan plan(MultimodalRequest request,
                       String enrichedQuestion,
                       RequestAnalysis analysis);
}
