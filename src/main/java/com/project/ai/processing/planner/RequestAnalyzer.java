package com.project.ai.processing.planner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.planner.AnalysisJson;
import com.project.ai.model.planner.ComplexityLevel;
import com.project.ai.model.planner.IntentType;
import com.project.ai.model.planner.RequestAnalysis;
import com.project.ai.util.LanguageDetector;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:36 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class RequestAnalyzer {

    @Qualifier("fastChatModel")
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;
    private final PromptLoader promptLoader;      // ← inject


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

    public RequestAnalysis analyze(MultimodalRequest request, String memoryContext) {


        TokenTracker tracker = request.getTokenTracker();
        String rawQuestion = request.getTextQuestion();

        // RequestAnalyzer.analyze() — add at the top before LLM call
        if (isGreeting(rawQuestion, request.getDetectedLanguage())) {
            log.info("[RequestAnalyzer] Greeting detected — skipping LLM call");
            return RequestAnalysis.builder()
                    .enrichedQuestion(rawQuestion)
                    .language(request.getDetectedLanguage())
                    .complexity(ComplexityLevel.SIMPLE)
                    .intentTypes(List.of(IntentType.KNOWLEDGE))
                    .isMultiStep(false)
                    .isAmbiguous(false)
                    .requiresMemoryContext(false)
                    .relatedToPreviousContext(false)
                    .normalizedQuestion(rawQuestion)
                    .searchType("greeting")    // ← new type
                    .category(null)
                    .brand(null)
                    .minPrice(null)
                    .maxPrice(null)
                    .sortDirection(null)
                    .build();
        }

        log.info("[RequestAnalyzer] START — question='{}'", rawQuestion);  // ← fix

        Language language = detectLanguage(rawQuestion);
        String normalized = rawQuestion.trim();                             // ← fix

        String memorySection = memoryContext.isBlank()
                ? "No previous conversation."
                : memoryContext;

        String promptKey = language == Language.ARABIC
                ? PromptKeys.REQUEST_ANALYZER_ARABIC
                : PromptKeys.REQUEST_ANALYZER_ENGLISH;

        String promptTemplate = promptLoader.get(promptKey);
        String prompt = promptTemplate.formatted(memorySection, rawQuestion);

        long start = System.currentTimeMillis();
        ChatResponse response = chatModel.chat(UserMessage.from(prompt));
        long duration = System.currentTimeMillis() - start;

        // ← add null check here
        log.info("[RequestAnalyzer] raw response: {}", response);
        log.info("[RequestAnalyzer] aiMessage: {}", response.aiMessage());
        log.info("[RequestAnalyzer] contents: {}", response.aiMessage().text());
        log.info("[RequestAnalyzer] contents: {}", response.aiMessage().attributes());

        String json = response.aiMessage().text()
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        try {

            tracker.record(
                    "request-analyzer",
                    properties.getChatModel().getModels().get("fast").getModelName(),
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount(),
                    duration
            );

            AnalysisJson parsed = parseAnalysis(json);

            String enriched = (parsed.enrichedQuestion() == null
                    || parsed.enrichedQuestion().isBlank())
                    ? rawQuestion
                    : parsed.enrichedQuestion();

            RequestAnalysis analysis = RequestAnalysis.builder()
                    .enrichedQuestion(enriched)
                    .language(language)
                    .complexity(ComplexityLevel.valueOf(parsed.complexity()))
                    .intentTypes(parsed.intentTypes().stream()
                            .map(IntentType::valueOf)
                            .toList())
                    .isMultiStep(parsed.isMultiStep())
                    .isAmbiguous(parsed.isAmbiguous())
                    .requiresMemoryContext(parsed.requiresMemoryContext())
                    .relatedToPreviousContext(parsed.relatedToPreviousContext())
                    .normalizedQuestion(rawQuestion)
                    .searchType(parsed.searchType())
                    .category(parsed.category())
                    .brand(parsed.brand())
                    .minPrice(parsed.minPrice())
                    .maxPrice(parsed.maxPrice())
                    .sortDirection(parsed.sortDirection())
                    .build();

            log.info("[RequestAnalyzer] enriched='{}' complexity={} intents={} multiStep={} ambiguous={}",
                    analysis.getEnrichedQuestion(), analysis.getComplexity(),
                    analysis.getIntentTypes(), analysis.isMultiStep(), analysis.isAmbiguous());

            return analysis;

        } catch (Exception e) {
            log.warn("[RequestAnalyzer] Failed to parse analysis, using fallback: {}", e.getMessage());
            return fallback(language, normalized);
        }
    }


    private Language detectLanguage(String question) {
        return LanguageDetector.detect(question);
    }

    private RequestAnalysis fallback(Language language, String rawQuestion) {
        log.warn("[RequestAnalyzer] using fallback for question='{}'", rawQuestion);
        return RequestAnalysis.builder()
                .enrichedQuestion(rawQuestion)
                .language(language)
                .complexity(ComplexityLevel.SIMPLE)
                .intentTypes(List.of(IntentType.KNOWLEDGE))
                .isMultiStep(false)
                .isAmbiguous(false)
                .requiresMemoryContext(false)
                .relatedToPreviousContext(false)
                .normalizedQuestion(rawQuestion)
                .searchType("knowledge")          // ← never null
                .category(null)
                .brand(null)
                .minPrice(null)
                .maxPrice(null)
                .sortDirection(null)
                .build();
    }

    private AnalysisJson parseAnalysis(String json) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        List<String> intents = new ArrayList<>();
        node.get("intentTypes").forEach(n -> intents.add(n.asText()));

        JsonNode enrichedNode = node.get("enrichedQuestion");
        String enrichedQuestion = (enrichedNode == null || enrichedNode.isNull())
                ? null : enrichedNode.asText();

        return new AnalysisJson(
                enrichedQuestion,
                node.get("complexity").asText(),
                intents,
                node.get("isMultiStep").asBoolean(),
                node.get("isAmbiguous").asBoolean(),
                node.get("requiresMemoryContext").asBoolean(),
                node.get("relatedToPreviousContext").asBoolean(),
                getTextOrNull(node, "searchType"),
                getTextOrNull(node, "category"),
                getTextOrNull(node, "brand"),
                getDoubleOrNull(node, "minPrice"),
                getDoubleOrNull(node, "maxPrice"),
                getTextOrNull(node, "sortDirection")
        );
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asDouble();
    }

    private boolean isGreeting(String question, Language language) {
        if (question == null) return false;
        String normalized = question.trim().toLowerCase();
        if (language == Language.ARABIC) {
            return ARABIC_GREETINGS.contains(normalized);
        }
        return ENGLISH_GREETINGS.contains(normalized);
    }
}
