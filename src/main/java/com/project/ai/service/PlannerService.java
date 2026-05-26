package com.project.ai.service;

import com.project.ai.dto.InputType;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.planner.IntentType;
import com.project.ai.model.planner.PlanResult;
import com.project.ai.model.planner.RequestAnalysis;
import com.project.ai.processing.planner.PlannerMemoryProcessor;
import com.project.ai.processing.planner.RequestAnalyzer;
import com.project.ai.strategy.BusinessStrategyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 24/05/2026
 * @Time: 12:10 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class PlannerService {

    private final RequestAnalyzer requestAnalyzer;
    private final TokenTrackerService tokenTrackerService;
    private final PlannerMemoryProcessor plannerMemoryProcessor;
    private final BusinessStrategyLoader strategyLoader;

    public MultimodalResponse plan(MultimodalRequest request) {

        log.info("[PlannerService] START — userId={} question='{}'",
                request.getUserId(), request.getTextQuestion());

        try {

            // Step 1 — load memory from DB (no LLM)
            String memoryContext = plannerMemoryProcessor.getMemoryContext(request);
            log.info("[PlannerService] memory loaded — hasMemory={}",
                    memoryContext != null && !memoryContext.isBlank());

            // Step 2 — enrich + analyze + tier in ONE LLM call
            RequestAnalysis analysis = requestAnalyzer.analyze(request, memoryContext); // TODO handle it to avoid NPE
            log.info("[PlannerService] analysis — enriched='{}' complexity={} multiStep={} ambiguous={}",
                    analysis.getEnrichedQuestion(),
                    analysis.getComplexity(),
                    analysis.isMultiStep(),
                    analysis.isAmbiguous());

            // Step 3 — set all analysis results on request
            request.setTextQuestion(analysis.getEnrichedQuestion());
            request.setMemoryContext(memoryContext);
            request.setRelatedToPreviousContext(analysis.isRelatedToPreviousContext());
            request.setSearchIntent(buildIntent(analysis));


            MultimodalResponse result = strategyLoader.getActive().handle(analysis, request);

            // Step 5 — persist token usage
            tokenTrackerService.persist(request.getTokenTracker());

            log.info("[PlannerService] END — userId={} type={}",
                    request.getUserId(), result.getType());

            return result;

        } catch (Exception e) {
            log.error("[PlannerService] FAILED — userId={} error={}", request.getUserId(), e.getMessage(), e);
            tokenTrackerService.persist(request.getTokenTracker());
            throw e;
        }
    }

    private SearchIntent buildIntent(RequestAnalysis analysis) {
        String searchType = analysis.getSearchType() != null
                ? analysis.getSearchType()
                : "knowledge";                    // ← never null

        return SearchIntent.builder()
                .searchType(searchType)
                .semanticQuery(analysis.getEnrichedQuestion())
                .category(analysis.getCategory())
                .brand(analysis.getBrand())
                .minPrice(analysis.getMinPrice())
                .maxPrice(analysis.getMaxPrice())
                .sortDirection(analysis.getSortDirection())
                .build();
    }

    private String cleanType(String type) {
        if (type == null) return "unknown";
        if (type.contains("+")) return "multi";
        return type;
    }
}
