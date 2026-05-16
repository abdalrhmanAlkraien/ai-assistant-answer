package com.project.ai.service;

import com.project.ai.agents.MultiAgentCoordinator;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.processing.text.InputProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:39 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ChatService {

    private final MultiAgentCoordinator multiAgentCoordinator;
    private final InputProcessor inputProcessor;

    public MultimodalResponse chat(final Long userId, final ChatRequest chatRequest) {

        log.info("[ChatService] START — userId ={}, question={}", userId, chatRequest.getQuestion());

        MultimodalRequest multimodalRequest = inputProcessor.process(userId, chatRequest);

        return multiAgentCoordinator.process(multimodalRequest);
    }
}
