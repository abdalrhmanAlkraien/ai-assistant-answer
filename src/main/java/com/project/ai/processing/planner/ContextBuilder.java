package com.project.ai.processing.planner;

import com.project.ai.model.planner.StoreContext;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 4:02 AM
 */
public interface ContextBuilder {
    StoreContext build();
    void invalidate();
}
