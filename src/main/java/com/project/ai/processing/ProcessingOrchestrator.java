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

    public ProcessingResult process(ProcessingRequest request) {

        memoryProcessor.prepareContext(request);
        SearchIntent intent = intentAnalyzer.extractIntent(request.getEnrichedQuestion());

        request.setSearchIntent(intent);

        ProcessingResult result = route(request);

        memoryProcessor.saveToMemory(request, result);

        return result;
    }

    private ProcessingResult route(ProcessingRequest request) {
        String type = request.getSearchIntent().getSearchType();

        // knowledge — no vector search needed
        if (knowledgeProcessor.supports(type)) {
            log.info("route to {}", type);
            return knowledgeProcessor.process(request);
        }

        // suggest — direct path (user explicitly asked for suggestions)
        if (suggestionProcessor.supports(type)) {
            log.info("route to {}", type);
            return suggestionProcessor.process(request);
        }

        // all other types — try segment first
        if (segmentProcessor.supports(type)) {
            log.info("route to {}", type);
            ProcessingResult result = segmentProcessor.process(request);

            // if segment found nothing → orchestrator decides to fallback
            if (result.getMatchedIds().isEmpty()) {
                log.info("SegmentProcessor returned empty — orchestrator falling back to SuggestionProcessor");
                return suggestionProcessor.process(request);
            }

            return result;
        }

        throw new IllegalStateException("No processor found for type: " + type);
    }
}
