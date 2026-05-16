package com.project.ai.processing.text.arabic;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import com.project.ai.processing.text.structure.ProcessingOrchestrator;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:41 PM
 */
@Service("arabicProcessingOrchestrator")
@Log4j2
public class ArabicProcessingOrchestrator implements ProcessingOrchestrator {

    private final ArabicMemoryProcessor memoryProcessor;
    private final IntentAnalyzer intentAnalyzer;

    // Arabic processors
    private final ChatProcessor knowledgeProcessor;
    private final ChatProcessor segmentProcessor;
    private final ChatProcessor sortProcessor;
    private final ChatProcessor suggestionProcessor;

    public ArabicProcessingOrchestrator(
            final ArabicMemoryProcessor memoryProcessor,
            final ArabicIntentAnalyzer intentAnalyzer,
            final ArabicKnowledgeProcessor knowledgeProcessor,
            final ArabicSegmentProcessor segmentProcessor,
            final ArabicSortProcessor sortProcessor,
            final ArabicSuggestionProcessor suggestionProcessor
    ) {
        this.memoryProcessor = memoryProcessor;
        this.intentAnalyzer = intentAnalyzer;
        this.knowledgeProcessor = knowledgeProcessor;
        this.segmentProcessor = segmentProcessor;
        this.sortProcessor = sortProcessor;
        this.suggestionProcessor = suggestionProcessor;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[ArabicProcessingOrchestrator] START — userId={}, question='{}'",
                request.getUserId(), request.getRawQuestion());

        memoryProcessor.prepareContext(request);

        SearchIntent intent = intentAnalyzer.extractIntent(request.getEnrichedQuestion());

        log.info("[ArabicProcessingOrchestrator] Parsed intent — type={}, category={}, brand={}, " +
                        "minPrice={}, maxPrice={}, sortDirection={}, semantic='{}'",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice(),
                intent.getSortDirection(), intent.getSemanticQuery());

        if (intent.getSemanticQuery() == null || intent.getSemanticQuery().isBlank()) {
            intent.setSemanticQuery(request.getEnrichedQuestion());
        }

        request.setSearchIntent(intent);

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

        String type = request.getSearchIntent().getSearchType();

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
}
