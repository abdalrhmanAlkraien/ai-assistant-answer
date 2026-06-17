package com.project.ai.processing.text.arabic;

import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.Product;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.EcommerceFilterProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
import com.project.ai.service.SuggestionService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:28 PM
 */
@Service
@Log4j2
public class ArabicSuggestionProcessor implements ChatProcessor {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SuggestionService suggestionService;
    private final FilterProcessor arabicFilterProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;
    private final EcommerceFilterProcessor ecommerceFilterProcessor;

    public ArabicSuggestionProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            final SuggestionService suggestionService,
            final FilterProcessor arabicFilterProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final SearchService searchService,
            @Qualifier("arabicChatModel") ChatModel chatModel,
            final LangChain4jProperties properties,
            final EcommerceFilterProcessor ecommerceFilterProcessor
    ) {

        this.embeddingStore = embeddingStore;
        this.suggestionService = suggestionService;
        this.matchedIdsResolver = matchedIdsResolver;
        this.arabicFilterProcessor = arabicFilterProcessor;
        this.searchService = searchService;
        this.chatModel = chatModel;
        this.properties = properties;
        this.ecommerceFilterProcessor = ecommerceFilterProcessor;
    }

    @Override
    public boolean supports(String searchType) {
        return "suggest".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        log.info("[ArabicSuggestionProcessor] START");

        SearchIntent originalIntent = request.getSearchIntent();

        // ── Build suggest intent — set excludedBrand from brand ──────────────
        SearchIntent suggestIntent = SearchIntent.builder()
                .searchType("suggest")
                .category(originalIntent.getCategory())
                .excludedBrand(originalIntent.getBrand())  // ← exclude original brand
                .maxPrice(originalIntent.getMaxPrice())
                .semanticQuery(originalIntent.getSemanticQuery())
                .build();

        // ── DB query — no vector search ───────────────────────────────────────
        FilteredContext context = ecommerceFilterProcessor.filter(suggestIntent);

        // ── No results found ──────────────────────────────────────────────────
        if (context.getProducts().isEmpty()) {
            log.info("[ArabicSuggestionProcessor] No alternatives found");
            return ProcessingResult.builder()
                    .enrichedQuestion(originalIntent.getSemanticQuery())
                    .type("suggest")
                    .answer("عذراً، لم نجد بدائل متاحة لطلبك في كتالوجنا حالياً.")
                    .matchedIds(List.of())
                    .build();
        }

        // ── Build prompt and call LLM ─────────────────────────────────────────
        String enrichedQuestion = request.getEnrichedQuestion() != null
                ? request.getEnrichedQuestion()
                : request.getRawQuestion();

        TokenTracker tracker = request.getTokenTracker();
        long start = System.currentTimeMillis();

        String question = suggestionService.suggestionProduct(
                enrichedQuestion, context, Language.ARABIC);
        ChatResponse answer = chatModel.chat(UserMessage.from(question));

        long duration = System.currentTimeMillis() - start;
        tracker.record("arabic-suggestion-processor",
                properties.getChatModel().getOllama().getArabicModelName(),
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                duration);

        List<String> matchedIds = context.getProducts().stream()
                .map(Product::getProductId)
                .toList();

        log.info("[ArabicSuggestionProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(originalIntent.getSemanticQuery())
                .type("suggest")
                .answer(answer.aiMessage().text())
                .matchedIds(matchedIds)
                .build();
    }

    private boolean isRelevantToQuery(FilteredContext context, SearchIntent originalIntent) {
        if (context.getFilteredMatches().isEmpty()) return false;

        String originalCategory = originalIntent.getCategory() != null
                ? originalIntent.getCategory().toLowerCase() : "";

        String originalQuery = originalIntent.getSemanticQuery() != null
                ? originalIntent.getSemanticQuery().toLowerCase() : "";

        if (!originalCategory.isBlank()) {
            boolean categoryMatch = context.getFilteredMatches().stream()
                    .anyMatch(m -> {
                        String productCategory = m.embedded().metadata().getString("category");
                        if (productCategory == null) return false;
                        String productCatLower = productCategory.toLowerCase();
                        // must match original category OR original query keywords
                        return productCatLower.contains(originalCategory)
                                || originalQuery.contains(productCatLower);
                    });

            if (!categoryMatch) {
                log.info("[ArabicSuggestionProcessor] No products match category='{}' or query='{}'",
                        originalCategory, originalQuery);
                return false;
            }
            return true;
        }

        return true;
    }
}
