package com.project.ai.processing;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:18 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessingOrchestrator {

    private final MemoryProcessor memoryProcessor;
    private final IntentAnalyzer intentAnalyzer;
    private final SegmentProcessor segmentProcessor;
    private final SuggestionProcessor suggestionProcessor;
    private final KnowledgeProcessor knowledgeProcessor;
    private final SortProcessor sortProcessor;

    public ProcessingResult process(ProcessingRequest request) {

        log.info("[Orchestrator] START — userId={}, question='{}'",
                request.getUserId(), request.getRawQuestion());

        memoryProcessor.prepareContext(request);

        SearchIntent intent = intentAnalyzer.extractIntent(request.getEnrichedQuestion());

        log.info("[Orchestrator] Parsed intent — type={}, category={}, brand={}, " +
                        "minPrice={}, maxPrice={}, sortDirection={}, semantic='{}'",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice(),
                intent.getSortDirection(), intent.getSemanticQuery());

        if (intent.getSemanticQuery() == null || intent.getSemanticQuery().isBlank()) {
            intent.setSemanticQuery(request.getEnrichedQuestion());
        }

        request.setSearchIntent(intent);

        ProcessingResult result = route(request);

        log.info("[Orchestrator] Result — type={}, matchedIds={}, answerLength={}",
                result.getType(), result.getMatchedIds().size(),
                result.getAnswer() != null ? result.getAnswer().length() : 0);

        memoryProcessor.saveToMemory(request, result);

        log.info("[Orchestrator] END — userId={}", request.getUserId());

        return result;
    }

    private ProcessingResult route(ProcessingRequest request) {
        String type = request.getSearchIntent().getSearchType();

        log.info("[Orchestrator] Start Routing process");

        // knowledge — no vector search needed
        if (knowledgeProcessor.supports(type)) {

            log.info("[Orchestrator] Routing to processor — type={}", type);
            return knowledgeProcessor.process(request);
        }

        // suggest — direct path (user explicitly asked for suggestions)
        if (suggestionProcessor.supports(type)) {

            log.info("[Orchestrator] Routing to processor — type={}", type);
            return suggestionProcessor.process(request);
        }

        if(sortProcessor.supports(type)) {
            return sortProcessor.process(request);
        }

        // all other types — try segment first
        if (segmentProcessor.supports(type)) {
            log.info("[Orchestrator] Routing to processor — type={}", type);
            ProcessingResult result = segmentProcessor.process(request);

            // if segment found nothing → orchestrator decides to fallback
            if (result.getMatchedIds().isEmpty()) {
                log.info("[Orchestrator] SegmentProcessor returned empty — orchestrator falling back to SuggestionProcessor");
                log.info("[Orchestrator] Routing to processor — type={}", type);
                return suggestionProcessor.process(request);
            }

            return result;
        }

        throw new IllegalStateException("No processor found for type: " + type);
    }
}
