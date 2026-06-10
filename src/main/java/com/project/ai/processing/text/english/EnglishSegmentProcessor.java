package com.project.ai.processing.text.english;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
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
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:26 PM
 */
@Service
@Log4j2
public class EnglishSegmentProcessor implements ChatProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            "semantic",     // best product for use case
            "price",        // price range filter
            "category",     // category listing
            "brand",        // brand listing
            "hybrid",       // combined filters
            "comparison"    // compare specific products
            // "sort"    → handled by EnglishSortProcessor before reaching here
            // "suggest" → handled by EnglishSuggestionProcessor before reaching here
            // "bundle"  → handled by Tier3 multi-step, never reaches here
            // "knowledge" → handled by EnglishKnowledgeProcessor before reaching here
    );

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final FilterProcessor filterProcessor;
    private final EnglishSuggestionProcessor englishSuggestionProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;
    private final LangChain4jProperties properties;
    private final PromptLoader promptLoader;

    public EnglishSegmentProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier("englishChatModel") final ChatModel chatModel,
            final FilterProcessor filterProcessor,
            final EnglishSuggestionProcessor englishSuggestionProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final SearchService searchService,
            final LangChain4jProperties properties,
            final PromptLoader promptLoader
    ) {

        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.filterProcessor = filterProcessor;
        this.englishSuggestionProcessor = englishSuggestionProcessor;
        this.matchedIdsResolver = matchedIdsResolver;
        this.searchService = searchService;
        this.properties = properties;
        this.promptLoader = promptLoader;
    }

    @Override
    public boolean supports(String searchType) {
        return SUPPORTED.contains(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[EnglishSegmentProcessor] START — type={}", request.getSearchIntent().getSearchType());
        SearchIntent intent = request.getSearchIntent();

        log.info("[EnglishSegmentProcessor] Building search request — semantic='{}'",
                intent.getSemanticQuery());

        EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(intent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        log.info("[EnglishSegmentProcessor] Vector search returned {} matches", matches.size());


        FilteredContext filteredContext = filterProcessor.filter(matches, intent);
        log.info("[EnglishSegmentProcessor] After filtering: {} products", filteredContext.getFilteredMatches().size());
        log.debug("[EnglishSegmentProcessor] Filtered products:\n{}", filteredContext.getContext());

        if (filteredContext.getFilteredMatches().isEmpty()) {
            log.info("[EnglishSegmentProcessor] No products after filtering — delegating to SuggestionProcessor");
            request.setVectorMatches(matches);
            return englishSuggestionProcessor.process(request);
        }

        String prompt = buildPrompt(intent, filteredContext,
                request.getRawQuestion(), request.getMemoryContext());

        log.debug("[EnglishSegmentProcessor] Prompt sent to LLM:\n{}", prompt);

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        ChatResponse answer = chatModel.chat(UserMessage.from(prompt));

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "english-knowledge-processor",
                properties.getChatModel().getModels().get("english").getModelName(),
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                intentDuration
        );

        log.debug("[EnglishSegmentProcessor] LLM answer:\n{}", answer.aiMessage().text());

        // 5. Extract matched IDs
        List<String> matchedIds = matchedIdsResolver.resolve(answer.aiMessage().text(), filteredContext, intent);
        log.info("[EnglishSegmentProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(answer.aiMessage().text())
                .matchedIds(matchedIds)
                .build();
    }


    private String buildPrompt(SearchIntent intent, FilteredContext filteredContext,
                               String userQuestion, String memoryContext) {

        String memorySection = (memoryContext != null && !memoryContext.isEmpty()
                && needsMemory(intent.getSearchType()))
                ? "User conversation history:\n%s\n\n".formatted(memoryContext)
                : "";

        String context = filteredContext.getContext();
        int count = filteredContext.getFilteredMatches().size();

        String promptKey = resolvePromptKey(intent.getSearchType());
        String template = promptLoader.get(promptKey);

        return switch (intent.getSearchType()) {

            case "price" -> template.formatted(
                    count, count, context, memorySection, count);

            case "category" -> template.formatted(
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(), context, memorySection);

            case "brand" -> template.formatted(
                    intent.getBrand(), context, memorySection);

            case "hybrid" -> template.formatted(
                    intent.getMinPrice() != null ? "- Price >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- Price <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- Brand: " + intent.getBrand() + "\n" : "",
                    context, memorySection);

            case "comparison" -> template.formatted(
                    context, memorySection, userQuestion);

            case "semantic" -> {

                boolean singleProduct = filteredContext.getFilteredMatches().size() == 1;
                String semanticMemory = singleProduct ? "" : memorySection;

                String priceOrSingleRule = singleProduct
                        ? "You have exactly ONE product. Describe why it matches the user request based ONLY on its listed features. Do NOT mention or compare with any other products."
                        : intent.getMaxPrice() != null
                        ? "Respect the price constraint — do NOT recommend products above $" + intent.getMaxPrice()
                        : "There is NO price constraint — recommend the BEST product regardless of price";

                yield template.formatted(
                        intent.getSemanticQuery(),
                        intent.getMaxPrice() != null ? "- Max price: $" + intent.getMaxPrice() + "\n" : "",
                        intent.getCategory() != null ? "- Category: " + intent.getCategory() + "\n" : "",
                        priceOrSingleRule,
                        context,
                        semanticMemory
                );
//                template.formatted(
//                        intent.getSemanticQuery(),
//                        intent.getMaxPrice() != null ? "- Max price: $" + intent.getMaxPrice() + "\n" : "",
//                        intent.getCategory() != null ? "- Category: " + intent.getCategory() + "\n" : "",
//                        intent.getMaxPrice() != null
//                                ? "Respect the price constraint — do NOT recommend products above $"
//                                + intent.getMaxPrice()
//                                : "There is NO price constraint — recommend the BEST product regardless of price",
//                        context, memorySection);
            }

            default -> {
                log.warn("[EnglishSegmentProcessor] unexpected searchType='{}' — using default prompt",
                        intent.getSearchType());
                yield template.formatted(context, memorySection, userQuestion);
            }
        };
    }

    private String resolvePromptKey(String searchType) {
        return switch (searchType) {
            case "price"      -> PromptKeys.SEGMENT_ENGLISH_PRICE;
            case "category"   -> PromptKeys.SEGMENT_ENGLISH_CATEGORY;
            case "brand"      -> PromptKeys.SEGMENT_ENGLISH_BRAND;
            case "hybrid"     -> PromptKeys.SEGMENT_ENGLISH_HYBRID;
            case "comparison" -> PromptKeys.SEGMENT_ENGLISH_COMPARISON;
            case "semantic"   -> PromptKeys.SEGMENT_ENGLISH_SEMANTIC;
            default           -> PromptKeys.SEGMENT_ENGLISH_DEFAULT;
        };
    }

    private boolean needsMemory(String searchType) {
        return switch (searchType) {
            case "comparison", "semantic", "knowledge", "suggest" -> true;
            default -> false;
        };
    }
}
