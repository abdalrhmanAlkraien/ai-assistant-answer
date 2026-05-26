package com.project.ai.processing.text.english.ecommerce;

import com.project.ai.dto.AiResult;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.normalizer.CategoryNormalizer;
import com.project.ai.processing.text.english.EnglishIntentAnalyzer;
import com.project.ai.processing.text.english.EnglishKnowledgeProcessor;
import com.project.ai.processing.text.english.EnglishMemoryProcessor;
import com.project.ai.processing.text.english.EnglishProcessingOrchestrator;
import com.project.ai.processing.text.english.EnglishSegmentProcessor;
import com.project.ai.processing.text.english.EnglishSortProcessor;
import com.project.ai.processing.text.english.EnglishSuggestionProcessor;
import com.project.ai.processing.text.structure.EcommerceFilterProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 26/05/2026
 * @Time: 4:22 AM
 */
@Service("ecommerceEnglishOrchestrator")
@RequiredArgsConstructor
@Log4j2
public class EcommerceEnglishOrchestrator implements EnglishProcessingOrchestrator {

    private static final Set<String> DB_TYPES = Set.of(
            "price", "category", "brand", "hybrid");

    private final EnglishMemoryProcessor englishMemoryProcessor;
    private final EnglishIntentAnalyzer englishIntentAnalyzer;
    private final EnglishSegmentProcessor englishSegmentProcessor;
    private final EnglishSuggestionProcessor englishSuggestionProcessor;
    private final EnglishKnowledgeProcessor englishKnowledgeProcessor;
    private final EnglishSortProcessor englishSortProcessor;
    private final CategoryNormalizer categoryNormalizer;
    private final EcommerceFilterProcessor filterProcessor;    // ← add


    @Override
    public ProcessingResult process(final ProcessingRequest request) {

        log.info("[EcommerceEnglishOrchestrator] START — userId={}, question='{}'",
                request.getUserId(), request.getRawQuestion());

        // skip intent extraction — already set by planner
        if (request.getSearchIntent() != null) {
            log.info("[EcommerceEnglishOrchestrator] intent already set type={} — skipping LLM call",
                    request.getSearchIntent().getSearchType());
        } else {
            log.info("[EcommerceEnglishOrchestrator] extracting intent via LLM");
            AiResult<SearchIntent> result = englishIntentAnalyzer.extractIntent(
                    request.getEnrichedQuestion());
            SearchIntent intent = result.result();
            if (intent.getSemanticQuery() == null || intent.getSemanticQuery().isBlank()) {
                intent.setSemanticQuery(request.getEnrichedQuestion());
            }
            request.setSearchIntent(intent);
        }

        // normalize category regardless of where intent came from
        SearchIntent intent = request.getSearchIntent();
        if (intent.getCategory() != null) {
            String normalized = categoryNormalizer.normalize(intent.getCategory());
            log.info("[EcommerceEnglishOrchestrator] category normalized: '{}' → '{}'",
                    intent.getCategory(), normalized);
            intent.setCategory(normalized);
        }

        log.info("[EcommerceEnglishOrchestrator] intent — type={} category={} brand={} " +
                        "minPrice={} maxPrice={} sortDirection={}",
                request.getSearchIntent().getSearchType(),
                request.getSearchIntent().getCategory(),
                request.getSearchIntent().getBrand(),
                request.getSearchIntent().getMinPrice(),
                request.getSearchIntent().getMaxPrice(),
                request.getSearchIntent().getSortDirection());


        ProcessingResult result = route(request);

        log.info("[EcommerceEnglishOrchestrator] Result — type={} matchedIds={} answerLength={}",
                result.getType(), result.getMatchedIds().size(),
                result.getAnswer() != null ? result.getAnswer().length() : 0);

        englishMemoryProcessor.saveToMemory(request, result);

        log.info("[EcommerceEnglishOrchestrator] END — userId={}", request.getUserId());
        return result;
    }
    @Override
    public ProcessingResult route(ProcessingRequest request) {

        String type = request.getSearchIntent() != null
                ? request.getSearchIntent().getSearchType()
                : "knowledge";                    // ← safe default

        if (type == null) type = "knowledge";     // ← extra null guard


        log.info("[EcommerceEnglishOrchestrator] routing — type={}", type);

        if (englishKnowledgeProcessor.supports(type)) {
            log.info("[EcommerceEnglishOrchestrator] → KnowledgeProcessor");
            return englishKnowledgeProcessor.process(request);
        }

        if (englishSuggestionProcessor.supports(type)) {
            log.info("[EcommerceEnglishOrchestrator] → SuggestionProcessor");
            return englishSuggestionProcessor.process(request);
        }

        if (englishSortProcessor.supports(type)) {
            log.info("[EcommerceEnglishOrchestrator] → SortProcessor");
            return englishSortProcessor.process(request);
        }

        // price, category, brand, hybrid — SQL direct
        if (DB_TYPES.contains(type)) {
            log.info("[EcommerceEnglishOrchestrator] → EcommerceFilterProcessor (SQL)");
            return handleDbFilter(request);
        }

        // semantic, comparison — vector search via SegmentProcessor
        if (englishSegmentProcessor.supports(type)) {
            log.info("[EcommerceEnglishOrchestrator] → SegmentProcessor");
            ProcessingResult result = englishSegmentProcessor.process(request);

            if (result.getMatchedIds().isEmpty()) {
                log.info("[EcommerceEnglishOrchestrator] empty — fallback to SuggestionProcessor");
                return englishSuggestionProcessor.process(request);
            }

            return result;
        }

        throw new IllegalStateException(
                "[EcommerceEnglishOrchestrator] No processor found for type: " + type);
    }

    private ProcessingResult handleDbFilter(ProcessingRequest request) {
        SearchIntent intent = request.getSearchIntent();

        log.info("[EcommerceEnglishOrchestrator] handleDbFilter — type={} category={} brand={} min={} max={}",
                intent.getSearchType(), intent.getCategory(), intent.getBrand(),
                intent.getMinPrice(), intent.getMaxPrice());

        FilteredContext context = filterProcessor.filter(intent);

        if (context.isEmpty()) {
            log.info("[EcommerceEnglishOrchestrator] SQL empty — fallback to SuggestionProcessor");
            return englishSuggestionProcessor.process(request);
        }

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(context.getContext())
                .matchedIds(context.getMatchedIds())
                .build();
    }
}
