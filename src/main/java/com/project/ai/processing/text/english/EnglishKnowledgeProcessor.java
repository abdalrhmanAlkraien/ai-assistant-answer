package com.project.ai.processing.text.english;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
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
    private final LangChain4jProperties properties;

    public EnglishKnowledgeProcessor(
            @Qualifier("englishChatModel") final ChatModel chatModel,
            final LangChain4jProperties properties) {
        this.chatModel = chatModel;
        this.properties = properties;
    }

    @Override
    public boolean supports(String searchType) {
        return "knowledge".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        log.info("[EnglishKnowledgeProcessor] START — question='{}'", request.getRawQuestion());

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        String question = """
                You are a helpful assistant.
                Answer this question based on your knowledge.
                Be concise and helpful.
                
                Question: %s
                """.formatted(request.getRawQuestion());

        ChatResponse answer = chatModel.chat(UserMessage.from(question));

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "english-knowledge-processor",
                properties.getChatModel().getOllama().getEnglishModelName(),
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                intentDuration
        );

        log.debug("[EnglishKnowledgeProcessor] LLM answer:\n{}", answer);

        log.info("[EnglishKnowledgeProcessor] END");

        return ProcessingResult.builder()
                .enrichedQuestion(request.getEnrichedQuestion())
                .type("knowledge")
                .answer(answer.aiMessage().text().trim())
                .matchedIds(List.of())
                .build();
    }
}
