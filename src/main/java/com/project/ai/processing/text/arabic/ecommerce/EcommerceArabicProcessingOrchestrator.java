package com.project.ai.processing.text.arabic.ecommerce;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.AiResult;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.normalizer.CategoryNormalizer;
import com.project.ai.processing.text.arabic.ArabicIntentAnalyzer;
import com.project.ai.processing.text.arabic.ArabicKnowledgeProcessor;
import com.project.ai.processing.text.arabic.ArabicMemoryProcessor;
import com.project.ai.processing.text.arabic.ArabicProcessingOrchestrator;
import com.project.ai.processing.text.arabic.ArabicSegmentProcessor;
import com.project.ai.processing.text.arabic.ArabicSortProcessor;
import com.project.ai.processing.text.arabic.ArabicSuggestionProcessor;
import com.project.ai.processing.text.structure.EcommerceFilterProcessor;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 4:30 AM
 */
@Service("arabicProcessingOrchestrator")
@Log4j2
public class EcommerceArabicProcessingOrchestrator implements ArabicProcessingOrchestrator {


    private final ArabicMemoryProcessor memoryProcessor;
    private final IntentAnalyzer intentAnalyzer;


    private static final Set<String> DB_TYPES = Set.of(
            "price", "category", "brand", "hybrid");

    // Arabic processors
    private final ChatProcessor knowledgeProcessor;
    private final ChatProcessor segmentProcessor;
    private final ChatProcessor sortProcessor;
    private final ChatProcessor suggestionProcessor;
    private final CategoryNormalizer categoryNormalizer;
    private final LangChain4jProperties properties;
    private final EcommerceFilterProcessor filterProcessor;    // ← add

    public EcommerceArabicProcessingOrchestrator(
            final ArabicMemoryProcessor memoryProcessor,
            final ArabicIntentAnalyzer intentAnalyzer,
            final ArabicKnowledgeProcessor knowledgeProcessor,
            final ArabicSegmentProcessor segmentProcessor,
            final ArabicSortProcessor sortProcessor,
            final ArabicSuggestionProcessor suggestionProcessor,
            final CategoryNormalizer categoryNormalizer,
            final LangChain4jProperties properties,
            EcommerceFilterProcessor filterProcessor
    ) {
        this.memoryProcessor = memoryProcessor;
        this.intentAnalyzer = intentAnalyzer;
        this.knowledgeProcessor = knowledgeProcessor;
        this.segmentProcessor = segmentProcessor;
        this.sortProcessor = sortProcessor;
        this.suggestionProcessor = suggestionProcessor;
        this.categoryNormalizer = categoryNormalizer;
        this.properties = properties;
        this.filterProcessor = filterProcessor;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[ArabicProcessingOrchestrator] START — userId={}, question='{}'",
                request.getUserId(), request.getRawQuestion());

        TokenTracker tracker = request.getTokenTracker();

        // skip intent extraction — already set by planner
        if (request.getSearchIntent() != null) {
            log.info("[EcommerceArabicOrchestrator] intent already set type={} — skipping LLM call",
                    request.getSearchIntent().getSearchType());
        } else {
            log.info("[EcommerceArabicOrchestrator] extracting intent via LLM");

            long intentStart = System.currentTimeMillis();
            AiResult<SearchIntent> responseResult = intentAnalyzer.extractIntent(
                    request.getEnrichedQuestion());
            long intentDuration = System.currentTimeMillis() - intentStart;

            tracker.record(
                    "arabic-intent-analysis",
                    properties.getChatModel().getOllama().getArabicModelName(),
                    responseResult.inputTokens(),
                    responseResult.outputTokens(),
                    intentDuration
            );

            SearchIntent intent = responseResult.result();
            if (intent.getSemanticQuery() == null || intent.getSemanticQuery().isBlank()) {
                intent.setSemanticQuery(request.getEnrichedQuestion());
            }
            request.setSearchIntent(intent);
        }

        // normalize category regardless of where intent came from
        SearchIntent intent = request.getSearchIntent();
        if (intent.getCategory() != null) {
            String normalized = categoryNormalizer.normalize(intent.getCategory());
            log.info("[EcommerceArabicOrchestrator] category normalized: '{}' → '{}'",
                    intent.getCategory(), normalized);
            intent.setCategory(normalized);
        }

        log.info("[ArabicProcessingOrchestrator] Parsed intent — type={}, category={}, brand={}, " +
                        "minPrice={}, maxPrice={}, sortDirection={}, semantic='{}', arabicSemantic = {}",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice(),
                intent.getSortDirection(), intent.getSemanticQuery(), intent.getSemanticQueryArabic());

        if (intent.getSemanticQuery() == null || intent.getSemanticQuery().isBlank()) {
            intent.setSemanticQuery(request.getEnrichedQuestion());
        }

        ProcessingResult result = route(request);

        log.info("[ArabicProcessingOrchestrator] Result — type={}, matchedIds={}, answerLength={}",
                result.getType(), result.getMatchedIds().size(),
                result.getAnswer() != null ? result.getAnswer().length() : 0);

        memoryProcessor.saveToMemory(request, result);

        log.info("[ArabicProcessingOrchestrator] END — userId={}", request.getUserId());

        return result;
    }

    @Override
    public ProcessingResult route(ProcessingRequest request) {

        String type = request.getSearchIntent() != null
                ? request.getSearchIntent().getSearchType()
                : "knowledge";                    // ← safe default

        if (type == null) type = "knowledge";     // ← extra null guard

        log.info("[ArabicProcessingOrchestrator] Start Routing process");

        // knowledge — no vector search needed
        if (knowledgeProcessor.supports(type)) {

            log.info("[ArabicProcessingOrchestrator] Routing to processor — type={}", type);
            return knowledgeProcessor.process(request);
        }

        // suggest — direct path (user explicitly asked for suggestions)
        if (suggestionProcessor.supports(type)) {

            log.info("[ArabicProcessingOrchestrator] Routing to processor — type={}", type);
            return suggestionProcessor.process(request);
        }

        if (sortProcessor.supports(type)) {
            return sortProcessor.process(request);
        }

        // price, category, brand, hybrid — SQL direct via EcommerceFilterProcessor
        if (DB_TYPES.contains(type)) {
            log.info("[EcommerceArabicOrchestrator] → EcommerceFilterProcessor (SQL)");
            return handleDbFilter(request);
        }

        // all other types — try segment first
        if (segmentProcessor.supports(type)) {
            log.info("[ArabicProcessingOrchestrator] Routing to processor — type={}", type);
            ProcessingResult result = segmentProcessor.process(request);

            // if segment found nothing → orchestrator decides to fallback
            if (result.getMatchedIds().isEmpty()) {
                log.info("[ArabicProcessingOrchestrator] SegmentProcessor returned empty — orchestrator falling back to SuggestionProcessor");
                log.info("[ArabicProcessingOrchestrator] Routing to processor — type={}", type);
                return suggestionProcessor.process(request);
            }

            return result;
        }

        throw new IllegalStateException("No processor found for type: " + type);
    }

    private ProcessingResult handleDbFilter(ProcessingRequest request) {
        SearchIntent intent = request.getSearchIntent();

        log.info("[EcommerceArabicOrchestrator] handleDbFilter — type={} category={} brand={} min={} max={}",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice());

        FilteredContext context = filterProcessor.filter(intent);

        if (context.isEmpty()) {
            log.info("[EcommerceArabicOrchestrator] SQL empty — fallback to SuggestionProcessor");
            return suggestionProcessor.process(request);
        }

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(context.getContext())
                .matchedIds(context.getMatchedIds())
                .build();
    }
}
