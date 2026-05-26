package com.project.ai.strategy.ecommerce;

import com.project.ai.agents.MultiAgentCoordinator;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.model.planner.RequestAnalysis;
import com.project.ai.strategy.ecommerce.tier.EcommerceTier0;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 11:35 PM
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class EcommerceTierRouter {

    private final MultiAgentCoordinator multiAgentCoordinator;
    private final EcommerceTier0 tier0;

    public MultimodalResponse route(RequestAnalysis analysis,
                                    MultimodalRequest request) {

        log.info("[EcommerceTierRouter] routing complexity={}",
                analysis.getComplexity());

        return switch (analysis.getComplexity()) {

            case NO_LLM -> {
                log.info("[EcommerceTierRouter] → Tier0 (pure Java)");
                yield tier0.execute(analysis, request);
            }

            case SIMPLE, MEDIUM, COMPLEX -> {
                log.info("[EcommerceTierRouter] → Agent ({})",
                        analysis.getComplexity());
                request.setMemoryContext(analysis.getEnrichedQuestion());
                yield multiAgentCoordinator.process(request);
            }
        };
    }
}
