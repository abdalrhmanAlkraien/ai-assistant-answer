package com.project.ai.service;

import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.ChatResponse;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.ProcessingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:39 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ChatService {

    private final ProcessingOrchestrator orchestrator;

    public ChatResponse chat(final Long userId, final ChatRequest chatRequest) {

        log.info("[ChatService] START — userId ={}, question={}", userId, chatRequest.getQuestion());

        ProcessingRequest request = ProcessingRequest.builder()
                .userId(userId)
                .rawQuestion(chatRequest.getQuestion())
                .build();

        ProcessingResult result = orchestrator.process(request);

        return ChatResponse.builder()
                .question(result.getEnrichedQuestion())
                .type(result.getType())
                .answer(result.getAnswer())
                .matchProducts(result.getMatchedIds())
                .responseTime(LocalDateTime.now())
                .build();
    }
}
