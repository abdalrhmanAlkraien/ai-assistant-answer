package com.project.ai.model.planner;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:30 PM
 */
public enum ComplexityLevel {

    NO_LLM,    // sort, filter, price range — pure DB
    SIMPLE,    // single search, category browse
    MEDIUM,    // semantic, recommendation
    COMPLEX    // comparison, knowledge, multi-step
}
