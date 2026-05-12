package com.project.ai.processing;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:51 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class KnowledgeProcessor implements ChatProcessor {

    private final ChatModel chatModel;


    @Override
    public boolean supports(String searchType) {
        return "knowledge".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        log.info("KnowledgeProcessor handling question: {}", request.getEnrichedQuestion());

        String answer = chatModel.chat("""
                You are a helpful assistant.
                Answer this question based on your knowledge.
                Be concise and helpful.
                
                Question: %s
                """.formatted(request.getRawQuestion()));

        return ProcessingResult.builder()
                .enrichedQuestion(request.getEnrichedQuestion())
                .type("knowledge")
                .answer(answer)
                .matchedIds(List.of())
                .build();
    }
}
