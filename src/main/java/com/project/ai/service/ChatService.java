package com.project.ai.service;

import com.project.ai.agents.MultiAgentCoordinator;
import com.project.ai.config.TokenTrackerFactory;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.dto.TokenTracker;
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
    private final TokenTrackerFactory trackerFactory;
    private final TokenTrackerService tokenTrackerService;

    public MultimodalResponse chat(final Long userId, final ChatRequest chatRequest) {

        log.info("[ChatService] START — userId ={}, question={}", userId, chatRequest.getQuestion());

        TokenTracker tracker = trackerFactory.create(
                String.valueOf(userId),
                "moonshotai/kimi-k2.6", // TODO should be dynamic
                chatRequest.getQuestion()
        );

        MultimodalRequest multimodalRequest = inputProcessor.process(userId, chatRequest);
        multimodalRequest.setTokenTracker(tracker);
        MultimodalResponse response = multiAgentCoordinator.process(multimodalRequest);
        tokenTrackerService.persist(tracker);
        return response;
    }
}
