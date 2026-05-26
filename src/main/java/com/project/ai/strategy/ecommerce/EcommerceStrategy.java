package com.project.ai.strategy.ecommerce;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.model.planner.ClarificationContext;
import com.project.ai.model.planner.ComplexityLevel;
import com.project.ai.model.planner.ExecutionPlan;
import com.project.ai.model.planner.PlanResult;
import com.project.ai.model.planner.RequestAnalysis;
import com.project.ai.processing.planner.AmbiguityResolver;
import com.project.ai.processing.planner.ExecutionPlanner;
import com.project.ai.processing.planner.PlanExecutor;
import com.project.ai.strategy.BusinessStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 11:34 PM
 */
@Component("ecommerceStrategy")
@RequiredArgsConstructor
@Log4j2
public class EcommerceStrategy implements BusinessStrategy {

    @Qualifier("ecommerceExecutionPlanner")
    private final ExecutionPlanner executionPlanner;
    @Qualifier("ecommercePlanExecutor")
    private final PlanExecutor ecommercePlanExecutor;

    private final EcommerceTierRouter tierRouter;
    @Qualifier("ecommerceAmbiguityResolver")
    private final AmbiguityResolver ambiguityResolver;

    @Override
    public String name() {
        return "ecommerce";
    }

    @Override
    public MultimodalResponse handle(RequestAnalysis analysis, MultimodalRequest request) {

        log.info("[EcommerceStrategy] START — complexity={} multiStep={}",
                analysis.getComplexity(), analysis.isMultiStep());

        // clarification — no tier, no plan
        if (analysis.isAmbiguous()) {
            log.info("[EcommerceStrategy] ambiguous — returning clarification");
            return handleClarification(request);
        }

        // Tier 0/1/2 — single step, route directly through tierRouter
        if (!isComplexPlan(analysis)) {
            log.info("[EcommerceStrategy] single step — routing directly to tier");
            return tierRouter.route(analysis, request);
        }

        log.info("[EcommerceStrategy] multi-step — building execution plan");
        ExecutionPlan plan = executionPlanner.plan(
                request,
                request.getTextQuestion(),    // ← already enriched, set in PlannerService
                analysis);

        return ecommercePlanExecutor.execute(plan, request);
    }

    private boolean isComplexPlan(RequestAnalysis analysis) {
        return analysis.isMultiStep();
    }

    private MultimodalResponse handleClarification(MultimodalRequest request) {
        ClarificationContext context = ambiguityResolver.resolve(request);

        return MultimodalResponse.builder()
                .type("clarification")
                .answer(context.getClarificationQuestion())
                .suggestedOptions(context.getSuggestedOptions().isEmpty() ? List.of() : context.getSuggestedOptions())
                .matchProducts(List.of())
                .language(request.getDetectedLanguage())
                .inputType(request.getInputType())
                .responseTime(LocalDateTime.now())
                .build();
    }

    private MultimodalResponse toMultimodalResponse(PlanResult result,
                                                    MultimodalRequest request) {
        return MultimodalResponse.builder()
                .type(result.getType())
                .answer(result.getAnswer())
                .matchProducts(result.getMatchedIds())
                .language(request.getDetectedLanguage())
                .inputType(request.getInputType())
                .suggestedOptions(result.isClarificationRequired()
                        && result.getClarificationContext() != null
                        ? result.getClarificationContext().getSuggestedOptions()
                        : List.of())
                .responseTime(LocalDateTime.now())
                .build();
    }
}
