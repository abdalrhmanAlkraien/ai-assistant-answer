package com.project.ai.processing.planner;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.model.planner.ExecutionPlan;
import com.project.ai.model.planner.PlanResult;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 12:16 AM
 */
public interface PlanExecutor {
    MultimodalResponse execute(ExecutionPlan plan, MultimodalRequest request);
}
