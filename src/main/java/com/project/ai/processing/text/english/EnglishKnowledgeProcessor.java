package com.project.ai.processing.text.english;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.ChatProcessor;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:51 PM
 */
@Service
@Log4j2
public class EnglishKnowledgeProcessor implements ChatProcessor {

    private final ChatModel chatModel;

    public EnglishKnowledgeProcessor(@Qualifier("englishChatModel") final ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public boolean supports(String searchType) {
        return "knowledge".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        log.info("[KnowledgeProcessor] START — question='{}'", request.getRawQuestion());

        String answer = chatModel.chat("""
                You are a helpful assistant.
                Answer this question based on your knowledge.
                Be concise and helpful.
                
                Question: %s
                """.formatted(request.getRawQuestion()));

        log.debug("[KnowledgeProcessor] LLM answer:\n{}", answer);

        log.info("[KnowledgeProcessor] END");

        return ProcessingResult.builder()
                .enrichedQuestion(request.getEnrichedQuestion())
                .type("knowledge")
                .answer(answer)
                .matchedIds(List.of())
                .build();
    }
}
