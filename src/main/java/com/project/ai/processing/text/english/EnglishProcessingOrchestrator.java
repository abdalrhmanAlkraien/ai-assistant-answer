package com.project.ai.processing.text.english;

import com.project.ai.dto.AiResult;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.text.structure.ProcessingOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:18 PM
 */
@Service("englishProcessingOrchestrator")
@RequiredArgsConstructor
@Log4j2
public class EnglishProcessingOrchestrator implements ProcessingOrchestrator {

    private final EnglishMemoryProcessor englishMemoryProcessor;
    private final EnglishIntentAnalyzer englishIntentAnalyzer;
    private final EnglishSegmentProcessor englishSegmentProcessor;
    private final EnglishSuggestionProcessor englishSuggestionProcessor;
    private final EnglishKnowledgeProcessor englishKnowledgeProcessor;
    private final EnglishSortProcessor englishSortProcessor;

    @Override
    public ProcessingResult process(final ProcessingRequest request) {

        log.info("[Orchestrator] START — userId={}, question='{}'",
                request.getUserId(), request.getRawQuestion());

        englishMemoryProcessor.prepareContext(request);

        AiResult<SearchIntent> responseResult = englishIntentAnalyzer.extractIntent(request.getEnrichedQuestion());

        SearchIntent intent = responseResult.result();

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

        englishMemoryProcessor.saveToMemory(request, result);

        log.info("[Orchestrator] END — userId={}", request.getUserId());

        return result;
    }

    @Override
    public ProcessingResult route(ProcessingRequest request) {
        String type = request.getSearchIntent().getSearchType();

        log.info("[Orchestrator] Start Routing process");

        // knowledge — no vector search needed
        if (englishKnowledgeProcessor.supports(type)) {

            log.info("[Orchestrator] Routing to processor — type={}", type);
            return englishKnowledgeProcessor.process(request);
        }

        // suggest — direct path (user explicitly asked for suggestions)
        if (englishSuggestionProcessor.supports(type)) {

            log.info("[Orchestrator] Routing to processor — type={}", type);
            return englishSuggestionProcessor.process(request);
        }

        if (englishSortProcessor.supports(type)) {
            return englishSortProcessor.process(request);
        }

        // all other types — try segment first
        if (englishSegmentProcessor.supports(type)) {
            log.info("[Orchestrator] Routing to processor — type={}", type);
            ProcessingResult result = englishSegmentProcessor.process(request);

            // if segment found nothing → orchestrator decides to fallback
            if (result.getMatchedIds().isEmpty()) {
                log.info("[Orchestrator] SegmentProcessor returned empty — orchestrator falling back to SuggestionProcessor");
                log.info("[Orchestrator] Routing to processor — type={}", type);
                return englishSuggestionProcessor.process(request);
            }

            return result;
        }

        throw new IllegalStateException("No processor found for type: " + type);
    }
}
