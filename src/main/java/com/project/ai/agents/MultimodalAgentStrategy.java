package com.project.ai.agents;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:08 PM
 */
public interface MultimodalAgentStrategy {

    boolean supports(MultimodalRequest request);
    MultimodalResponse process(MultimodalRequest request);
}
