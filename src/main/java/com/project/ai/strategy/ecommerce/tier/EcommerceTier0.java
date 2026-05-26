package com.project.ai.strategy.ecommerce.tier;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 11:42 PM
 */

import com.project.ai.agents.MultiAgentCoordinator;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.model.planner.RequestAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class EcommerceTier0 {


    private final MultiAgentCoordinator multiAgentCoordinator;

    public MultimodalResponse execute(RequestAnalysis analysis,
                                      MultimodalRequest request) {

        log.info("[EcommerceTier0] START — intents={} related={}",
                analysis.getIntentTypes(), analysis.isRelatedToPreviousContext());

        return multiAgentCoordinator.process(request);
    }
}
