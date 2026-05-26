package com.project.ai.processing.text.arabic;

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
 * @Date: 16/05/2026
 * @Time: 11:42 PM
 */
@Service
@Log4j2
public class ArabicSegmentProcessor implements ChatProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            "semantic", "price", "category", "brand", "hybrid", "comparison", "sort");

    private static final String LANGUAGE_SYSTEM_HEADER = """
        أنت مساعد تسوق إلكتروني. تحدث العربية الفصحى فقط.
        قواعد غير قابلة للكسر:
        - استخدم اللغة العربية الفصحى في جميع ردودك دون استثناء
        - ممنوع تمامًا استخدام الروسية أو الصينية أو أي لغة أخرى
        - أسماء المنتجات والأرقام والعلامات التجارية تُكتب كما هي بالإنجليزية فقط
        - إذا لم تجد كلمة عربية مناسبة، استخدم التعريب
        
        """;

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final FilterProcessor filterProcessor;       // EnglishFilterProcessor via @Primary
    private final MatchedIdsResolver matchedIdsResolver; // EnglishMatchedIdsResolver via @Primary
    private final ChatProcessor suggestionProcessor;
    private final SearchService searchService;
    private final LangChain4jProperties properties;
    private final PromptLoader promptLoader;

    public ArabicSegmentProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier("arabicChatModel") ChatModel chatModel,
            final FilterProcessor filterProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final ArabicSuggestionProcessor suggestionProcessor,
            final SearchService searchService,
            final LangChain4jProperties properties,
            final PromptLoader promptLoader
    ) {
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.filterProcessor = filterProcessor;
        this.matchedIdsResolver = matchedIdsResolver;
        this.suggestionProcessor = suggestionProcessor;
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

        log.info("[ArabicSegmentProcessor] START — type={}", request.getSearchIntent().getSearchType());
        SearchIntent intent = request.getSearchIntent();

        log.info("[ArabicSegmentProcessor] Building search request — semantic='{}'",
                intent.getSemanticQuery());

        EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(intent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        log.info("[ArabicSegmentProcessor] Vector search returned {} matches", matches.size());

        FilteredContext filteredContext = filterProcessor.filter(matches, intent);
        log.info("[ArabicSegmentProcessor] After filtering: {} products", filteredContext.getFilteredMatches().size());
        log.debug("[ArabicSegmentProcessor] Filtered products:\n{}", filteredContext.getContext());

        if (filteredContext.getFilteredMatches().isEmpty()) {
            log.info("[ArabicSegmentProcessor] No products after filtering — delegating to SuggestionProcessor");
            request.setVectorMatches(matches);
            return suggestionProcessor.process(request);
        }

        String prompt = buildPrompt(intent, filteredContext,
                request.getRawQuestion(), request.getMemoryContext());

        log.debug("[ArabicSegmentProcessor] Prompt sent to LLM:\n{}", prompt);

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        ChatResponse answer = chatModel.chat(UserMessage.from(prompt));

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "arabic-segment-processor",
                properties.getChatModel().getOllama().getArabicModelName(),
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                intentDuration
        );

        log.debug("[ArabicSegmentProcessor] LLM answer:\n{}", answer.aiMessage().text());

        List<String> matchedIds = matchedIdsResolver.resolve(answer.aiMessage().text(), filteredContext, intent);
        log.info("[ArabicSegmentProcessor] END — matchedIds={}", matchedIds);

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
                ? "سجل المحادثة مع المستخدم:\n%s\n\n".formatted(memoryContext)
                : "";

        String context = filteredContext.getContext();
        int count = filteredContext.getFilteredMatches().size();

        String promptKey = resolvePromptKey(intent.getSearchType());
        String template = promptLoader.get(promptKey);

        return switch (intent.getSearchType()) {

            case "price" -> template.formatted(
                    count, count, context, memorySection);

            case "category" -> template.formatted(
                    intent.getCategory(), intent.getCategory(),
                    context, memorySection);

            case "brand" -> template.formatted(
                    intent.getBrand(), context, memorySection);

            case "hybrid" -> template.formatted(
                    intent.getMinPrice() != null ? "- السعر >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- السعر <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- العلامة التجارية: " + intent.getBrand() + "\n" : "",
                    context, memorySection);

            case "comparison" -> template.formatted(
                    context, memorySection, userQuestion);

            case "semantic" -> template.formatted(
                    intent.getSemanticQuery(),
                    intent.getMaxPrice() != null ? "- الحد الأقصى للسعر: $" + intent.getMaxPrice() + "\n" : "",
                    intent.getCategory() != null ? "- الفئة: " + intent.getCategory() + "\n" : "",
                    intent.getMaxPrice() != null
                            ? "التزم بحد السعر — لا تُوصِ بمنتجات تتجاوز $" + intent.getMaxPrice()
                            : "لا يوجد قيد على السعر — أوصِ بأفضل منتج بغض النظر عن السعر",
                    context, memorySection);

            default -> {
                log.warn("[ArabicSegmentProcessor] unexpected type='{}' — using default",
                        intent.getSearchType());
                yield template.formatted(context, memorySection, userQuestion);
            }
        };
    }

    private String resolvePromptKey(String searchType) {
        return switch (searchType) {
            case "price"      -> PromptKeys.SEGMENT_ARABIC_PRICE;
            case "category"   -> PromptKeys.SEGMENT_ARABIC_CATEGORY;
            case "brand"      -> PromptKeys.SEGMENT_ARABIC_BRAND;
            case "hybrid"     -> PromptKeys.SEGMENT_ARABIC_HYBRID;
            case "comparison" -> PromptKeys.SEGMENT_ARABIC_COMPARISON;
            case "semantic"   -> PromptKeys.SEGMENT_ARABIC_SEMANTIC;
            default           -> PromptKeys.SEGMENT_ARABIC_DEFAULT;
        };
    }

    private boolean needsMemory(String searchType) {
        return switch (searchType) {
            case "comparison", "semantic", "knowledge", "suggest" -> true;
            default -> false;
        };
    }
}
