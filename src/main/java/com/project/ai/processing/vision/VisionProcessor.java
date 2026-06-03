package com.project.ai.processing.vision;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.TokenTracker;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 5:17 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class VisionProcessor {

    @Qualifier("vision")
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;


    public String describe(String imageBase64, String mediaType, TokenTracker tracker) {
        log.info("[VisionProcessor] START — mediaType={}", mediaType);

        try {
            String prompt = """
                    You are a product search assistant for an e-commerce store.
                    Analyze this image and describe what product(s) you see in terms useful for searching.
                    Focus on: product type, brand if visible, color, key features.
                    Be concise — 1-2 sentences maximum.
                    Respond in English only.
                    """;

            UserMessage message = UserMessage.from(
                    ImageContent.from(imageBase64, mediaType),
                    TextContent.from(prompt)
            );

            long start = System.currentTimeMillis();
            ChatResponse response = chatModel.chat(message);
            long duration = System.currentTimeMillis() - start;

            tracker.record(
                    "vision-processor",
                    properties.getChatModel().getModels().get("vision").getModelName(),
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount(),
                    duration
            );

            String description = response.aiMessage().text().trim();
            log.info("[VisionProcessor] END — description='{}'", description);
            return description;

        } catch (Exception e) {
            log.error("[VisionProcessor] Failed to describe image: {}", e.getMessage());
            throw new RuntimeException("Image description failed: " + e.getMessage());
        }
    }
}
