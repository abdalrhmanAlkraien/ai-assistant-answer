package com.project.ai.agents;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:43 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class MultiAgentCoordinator {

    private final List<MultimodalAgentStrategy> orchestrators;

    /**
     * Entry point from ChatService — routes by language + inputType.
     */
    public MultimodalResponse process(MultimodalRequest request) {
        log.info("[MultiAgentCoordinator] START — language={}, inputType={}",
                request.getDetectedLanguage(), request.getInputType());

        MultimodalResponse response = route(request);

        log.info("[MultiAgentCoordinator] END — type={}, language={}",
                response.getType(), response.getLanguage());

        return response;
    }

    public MultimodalResponse delegate(MultimodalRequest request) {
        log.info("[MultiAgentCoordinator] DELEGATE — language={}, inputType={}",
                request.getDetectedLanguage(), request.getInputType());

        return route(request);
    }

    private MultimodalResponse route(MultimodalRequest request) {
        return orchestrators.stream()
                .filter(o -> o.supports(request))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No orchestrator found for language=%s inputType=%s"
                                .formatted(request.getDetectedLanguage(),
                                        request.getInputType())))
                .process(request);
    }
}
