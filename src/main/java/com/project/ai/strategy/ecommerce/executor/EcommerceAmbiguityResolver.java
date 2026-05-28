package com.project.ai.strategy.ecommerce.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.MessageRole;
import com.project.ai.model.planner.ClarificationContext;
import com.project.ai.model.planner.EcommerceStoreContext;
import com.project.ai.processing.planner.AmbiguityResolver;
import com.project.ai.service.MemoryService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 5:27 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EcommerceAmbiguityResolver implements AmbiguityResolver {

    @Qualifier("fastChatModel")
    private final ChatModel fastChatModel;
    private final LangChain4jProperties properties;
    private final EcommerceContextBuilder contextBuilder;
    private final MemoryService memoryService;
    private final PromptLoader promptLoader;


    public ClarificationContext resolve(MultimodalRequest request) {
        log.info("[EcommerceAmbiguityResolver] START — question='{}' language={}",
                request.getTextQuestion(), request.getDetectedLanguage());

        EcommerceStoreContext storeContext = contextBuilder.build();
        TokenTracker tracker = request.getTokenTracker();

        List<String> suggestedOptions = pickOptions(storeContext, request.getDetectedLanguage());

        String categories;
        if (request.getDetectedLanguage() == Language.ARABIC) {
            categories = storeContext.getCategoryArabicNames().values().stream()
                    .sorted()
                    .collect(Collectors.joining("\n- ", "- ", ""));
        } else {
            categories = storeContext.getAvailableCategories().stream()
                    .sorted()
                    .collect(Collectors.joining("\n- ", "- ", ""));
        }

        // load prompt from DB
        String promptKey = request.getDetectedLanguage() == Language.ARABIC
                ? PromptKeys.CLARIFICATION_ARABIC
                : PromptKeys.CLARIFICATION_ENGLISH;

        String prompt = promptLoader.get(promptKey)
                .formatted(request.getTextQuestion(), categories);

        long start = System.currentTimeMillis();
        ChatResponse response = fastChatModel.chat(UserMessage.from(prompt));
        long duration = System.currentTimeMillis() - start;

        tracker.record(
                "ambiguity-resolver",
                properties.getChatModel().getModels().get("fast").getModelName(),
                response.tokenUsage().inputTokenCount(),
                response.tokenUsage().outputTokenCount(),
                duration
        );

        String json = response.aiMessage().text()
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        try {
            ObjectMapper mapper = new ObjectMapper();
            String clarificationQuestion = mapper.readTree(json)
                    .get("clarificationQuestion").asText();

            ClarificationContext context = ClarificationContext.builder()
                    .originalQuestion(request.getTextQuestion())
                    .clarificationQuestion(clarificationQuestion)
                    .suggestedOptions(suggestedOptions)
                    .language(request.getDetectedLanguage())
                    .build();

            saveToMemory(request.getUserId(), request.getTextQuestion(), context, request.getDetectedLanguage());

            log.info("[EcommerceAmbiguityResolver] clarification='{}'",
                    context.getClarificationQuestion());

            return context;

        } catch (Exception e) {
            log.warn("[EcommerceAmbiguityResolver] Failed to parse — fallback: {}", e.getMessage());
            return fallback(request.getTextQuestion(), request.getDetectedLanguage(),
                    suggestedOptions);
        }
    }

// ── Pick 4 options directly from DB ──────────────────────────────────────

    private List<String> pickOptions(EcommerceStoreContext context, Language language) {
        if (language == Language.ARABIC) {
            List<String> arabicNames = new ArrayList<>(
                    context.getCategoryArabicNames().values());
            Collections.shuffle(arabicNames);
            return arabicNames.stream().limit(4).toList();
        } else {
            List<String> slugs = new ArrayList<>(context.getAvailableCategories());
            Collections.shuffle(slugs);
            return slugs.stream()
                    .limit(4)
                    .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                    .toList();
        }
    }

    private ClarificationContext fallback(String question, Language language,
                                          List<String> suggestedOptions) {
        String q = language == Language.ARABIC
                ? "ما نوع المنتج الذي تبحث عنه؟"
                : "What type of product are you looking for?";

        return ClarificationContext.builder()
                .originalQuestion(question)
                .clarificationQuestion(q)
                .suggestedOptions(suggestedOptions)
                .language(language)
                .build();
    }

    // ── Save to memory ────────────────────────────────────────────────────────

    private void saveToMemory(Long userId, String question, ClarificationContext context, Language detectedLanguage) {
        try {
            memoryService.saveMemory(userId, "Ambiguity", MessageRole.USER, question, null, detectedLanguage);

            String aiMessage = context.getClarificationQuestion() + " "
                    + String.join(" | ", context.getSuggestedOptions());

            memoryService.saveMemory(userId, "Ambiguity", MessageRole.AI, aiMessage, null, detectedLanguage);

            log.info("[EcommerceAmbiguityResolver] saved to memory userId={}", userId);

        } catch (Exception e) {
            log.warn("[EcommerceAmbiguityResolver] failed to save to memory: {}", e.getMessage());
        }
    }
}
