package com.project.ai.strategy;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.model.planner.RequestAnalysis;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 11:34 PM
 */
public interface BusinessStrategy {

    String name();

    /**
     * Main entry point — strategy decides internally
     * whether to use tier directly or build a plan
     */
    MultimodalResponse handle(RequestAnalysis analysis, MultimodalRequest request);
}
