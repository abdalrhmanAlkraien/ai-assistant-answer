package com.project.ai.processing.text.arabic;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.ChatProcessor;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:21 PM
 */
@Service
@Log4j2
public class ArabicKnowledgeProcessor implements ChatProcessor {

    private final ChatModel chatModel;

    public ArabicKnowledgeProcessor(
            @Qualifier("arabicChatModel") ChatModel chatModel
    ) {
        this.chatModel = chatModel;
    }

    @Override
    public boolean supports(String searchType) {
        return "knowledge".equals(searchType);
    }


    @Override
    public ProcessingResult process(ProcessingRequest request) {
        log.info("[ArabicKnowledgeProcessor] START");

        String answer = chatModel.chat("""
                أنت مساعد مفيد لمنصة تجارة إلكترونية.
                أجب على هذا السؤال بناءً على معرفتك.
                كن مختصراً ومفيداً.
                أجب دائماً باللغة العربية.
                
                السؤال: %s
                
               تذكر: الإجابة باللغة العربية فقط.
                
                """.formatted(request.getRawQuestion()));

        log.info("[ArabicKnowledgeProcessor] END");

        return ProcessingResult.builder()
                .enrichedQuestion(request.getEnrichedQuestion())
                .type("knowledge")
                .answer(answer)
                .matchedIds(List.of())
                .build();
    }
}
