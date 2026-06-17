package com.project.ai.processing.text.english;

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
 * @Date: 12/05/2026
 * @Time: 9:24 PM
 */
@Service
@Log4j2
public class EnglishSuggestionProcessor implements ChatProcessor {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SuggestionService suggestionService;
    private final FilterProcessor filterProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;
    private final EcommerceFilterProcessor ecommerceFilterProcessor;

    public EnglishSuggestionProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            final SuggestionService suggestionService,
            final FilterProcessor filterProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final SearchService searchService,
            @Qualifier("englishChatModel") ChatModel chatModel,
            final LangChain4jProperties properties,
            EcommerceFilterProcessor ecommerceFilterProcessor
    ) {

        this.embeddingStore = embeddingStore;
        this.suggestionService = suggestionService;
        this.matchedIdsResolver = matchedIdsResolver;
        this.filterProcessor = filterProcessor;
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
        log.info("[EnglishSuggestionProcessor] START");

        SearchIntent originalIntent = request.getSearchIntent();

        SearchIntent suggestIntent = SearchIntent.builder()
                .searchType("suggest")
                .category(originalIntent.getCategory())
                .excludedBrand(originalIntent.getBrand())  // brand to exclude
                .maxPrice(originalIntent.getMaxPrice())
                .semanticQuery(originalIntent.getSemanticQuery())
                .build();

        FilteredContext context = ecommerceFilterProcessor.filter(suggestIntent);

        if (context.getProducts().isEmpty()) {
            log.info("[EnglishSuggestionProcessor] No alternatives found");
            return ProcessingResult.builder()
                    .enrichedQuestion(originalIntent.getSemanticQuery())
                    .type("suggest")
                    .answer("Sorry, we couldn't find any alternatives matching your request in our catalog.")
                    .matchedIds(List.of())
                    .build();
        }

        String enrichedQuestion = request.getEnrichedQuestion() != null
                ? request.getEnrichedQuestion()
                : request.getRawQuestion();

        TokenTracker tracker = request.getTokenTracker();
        long start = System.currentTimeMillis();

        String prompt = suggestionService.suggestionProduct(
                enrichedQuestion, context, Language.ENGLISH);
        ChatResponse answer = chatModel.chat(UserMessage.from(prompt));

        long duration = System.currentTimeMillis() - start;
        tracker.record("english-suggestion-processor",
                properties.getChatModel().getOllama().getEnglishModelName(),
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                duration);

        List<String> matchedIds = context.getProducts().stream()
                .map(Product::getProductId)
                .toList();

        log.info("[EnglishSuggestionProcessor] END — matchedIds={}", matchedIds);

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

        if (!originalCategory.isBlank()) {
            boolean categoryMatch = context.getFilteredMatches().stream()
                    .anyMatch(m -> {
                        String productCategory = m.embedded().metadata().getString("category");
                        if (productCategory == null) return false;
                        // category-only check — no query fallback
                        return productCategory.toLowerCase().contains(originalCategory);
                    });

            if (!categoryMatch) {
                log.info("[EnglishSuggestionProcessor] No products match category='{}'",
                        originalCategory);
                return false;
            }
            return true;
        }

        return true;
    }
}
