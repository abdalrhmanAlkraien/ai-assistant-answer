package com.project.ai.model.planner;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:32 PM
 */
@Builder
@Getter
public class ExecutionStep {

    private final String stepId;
    private final String stepName;
    private final String goal;
    private final AgentType agentType;
    private final String modelName;
    private final IntentType intentType;
    private final ComplexityLevel complexity;
    private final boolean canRunParallel;
    private final String dependsOnStepId;   // null if no dependency
    private final Map<String, Object> parameters;
    private String category;    // ← add
    private String brand;       // ← add
}
