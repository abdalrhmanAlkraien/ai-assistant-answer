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
import java.util.Set;
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

    private static final Set<String> ENGLISH_GREETINGS = Set.of(
            "hi", "hello", "hey", "good morning", "good afternoon", "good evening",
            "how are you", "how are you?", "thanks", "thank you", "bye", "goodbye",
            "welcome", "greetings", "sup", "what's up", "whats up"
    );

    private static final Set<String> ARABIC_GREETINGS = Set.of(
            "مرحبا", "أهلا", "أهلاً", "اهلا", "السلام عليكم", "صباح الخير",
            "مساء الخير", "كيف حالك", "كيف حالك؟", "شكرا", "شكراً", "وداعا",
            "مع السلامة", "أهلاً وسهلاً", "هلا", "هلو", "هاي"
    );


    public ClarificationContext resolve(MultimodalRequest request) {
        log.info("[EcommerceAmbiguityResolver] START — question='{}' language={}",
                request.getTextQuestion(), request.getDetectedLanguage());

        // ── Handle greetings without LLM call ────────────────────────────────
        if (isGreeting(request.getTextQuestion(), request.getDetectedLanguage())) {
            log.info("[EcommerceAmbiguityResolver] Greeting detected — skipping LLM");
            String greetingResponse = buildGreetingResponse(request.getDetectedLanguage());
            return ClarificationContext.builder()
                    .originalQuestion(request.getTextQuestion())
                    .clarificationQuestion(greetingResponse)
                    .suggestedOptions(List.of())
                    .language(request.getDetectedLanguage())
                    .isGreeting(true)
                    .build();
        }

        // ── Handle edge cases without LLM call ───────────────────────────────
        if (isEdgeCase(request.getTextQuestion())) {
            log.info("[EcommerceAmbiguityResolver] Edge case detected — skipping LLM");
            String edgeResponse = request.getDetectedLanguage() == Language.ARABIC
                    ? "عذراً، لم أفهم طلبك. هل يمكنك توضيح ما تبحث عنه؟"
                    : "Sorry, I didn't understand your request. Could you tell me what you're looking for?";

            EcommerceStoreContext storeContext = contextBuilder.build();
            List<String> options = pickOptions(storeContext, request.getDetectedLanguage());

            return ClarificationContext.builder()
                    .originalQuestion(request.getTextQuestion())
                    .clarificationQuestion(edgeResponse)
                    .suggestedOptions(options)
                    .language(request.getDetectedLanguage())
                    .isGreeting(false)
                    .build();
        }

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

    // ── Save to memory ────────────────────────────────────────────────────────

    private void saveToMemory(String userId, String question, ClarificationContext context, Language detectedLanguage) {
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

    private boolean isGreeting(String question, Language language) {
        if (question == null) return false;
        String normalized = question.trim().toLowerCase();
        if (language == Language.ARABIC) {
            return ARABIC_GREETINGS.contains(normalized);
        }
        return ENGLISH_GREETINGS.contains(normalized);
    }

    private String buildGreetingResponse(Language language) {
        if (language == Language.ARABIC) {
            return "أهلاً وسهلاً! كيف يمكنني مساعدتك في التسوق اليوم؟ يمكنني مساعدتك في إيجاد المنتجات والمقارنة بينها.";
        }
        return "Hello! Welcome to our store. How can I help you find products today? I can help you search, compare, and discover items.";
    }

    // ── Edge case detection ───────────────────────────────────────────────────

    private boolean isEdgeCase(String question) {
        if (question == null || question.isBlank()) return true;
        String trimmed = question.trim();

        // too short
        if (trimmed.length() <= 2) return true;

        // only numbers
        if (trimmed.matches("^[0-9\\s]+$")) return true;

        // only special characters
        if (trimmed.matches("^[^a-zA-Z\u0600-\u06FF0-9]+$")) return true;

        // random characters (no vowels in long string)
        if (trimmed.length() > 4 && trimmed.matches("^[bcdfghjklmnpqrstvwxyz]{5,}$")) return true;

        return false;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<String> pickOptions(EcommerceStoreContext context, Language language) {
        if (language == Language.ARABIC) {
            List<String> arabicNames = new ArrayList<>(context.getCategoryArabicNames().values());
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
                .isGreeting(false)
                .build();
    }
}
