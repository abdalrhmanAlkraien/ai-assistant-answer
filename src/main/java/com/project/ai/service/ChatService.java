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

        /**
         * Type of search on the vector DB is
         * 1- Semantic Search
         * 2- Exact search
         * 3- Range filter
         */

        /**
         * Question what is the  TextSegment here   private final EmbeddingStore<TextSegment> embeddingStore;
         * what is the type of EmbeddingStore?
         */
        /**
         * 1-  we need embeded questino becouse the data store as a vectors
         * 2-
         */
    }
}
