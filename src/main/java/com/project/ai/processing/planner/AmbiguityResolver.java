package com.project.ai.processing.planner;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.model.planner.ClarificationContext;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 11:46 PM
 */

public interface AmbiguityResolver {

    ClarificationContext resolve(MultimodalRequest request);
}
