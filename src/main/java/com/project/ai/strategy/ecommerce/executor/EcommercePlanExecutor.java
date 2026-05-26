package com.project.ai.strategy.ecommerce.executor;

import com.project.ai.agents.Language;
import com.project.ai.agents.MultiAgentCoordinator;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.dto.SearchIntent;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.MessageRole;
import com.project.ai.model.planner.ExecutionPlan;
import com.project.ai.model.planner.ExecutionStep;
import com.project.ai.model.planner.IntentType;
import com.project.ai.model.planner.PlanResult;
import com.project.ai.processing.planner.PlanExecutor;
import com.project.ai.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 11:45 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EcommercePlanExecutor implements PlanExecutor {

    private final MultiAgentCoordinator coordinator;
    private final MemoryService memoryService;

    private static final int THREAD_POOL_SIZE = 4;
    private final ExecutorService parallelExecutor =
            Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public MultimodalResponse execute(ExecutionPlan plan,
                                      MultimodalRequest request) {

        TokenTracker tracker = request.getTokenTracker();
        log.info("[PlanExecutor] START — steps={} parallel={} clarification={}",
                plan.getSteps().size(),
                plan.getSteps().stream().filter(ExecutionStep::isCanRunParallel).count(),
                plan.isRequiresClarification());

        // ── Group steps: parallel vs sequential ───────────────────────────────
        List<ExecutionStep> parallelSteps = plan.getSteps().stream()
                .filter(ExecutionStep::isCanRunParallel)
                .toList();
        List<ExecutionStep> sequentialSteps = plan.getSteps().stream()
                .filter(s -> !s.isCanRunParallel())
                .toList();

        List<MultimodalResponse> results = new ArrayList<>();
        int stepsExecuted = 0;
        boolean fallbackUsed = false;

        // ── Run parallel steps ────────────────────────────────────────────────
        if (!parallelSteps.isEmpty()) {
            log.info("[PlanExecutor] Running {} steps in parallel", parallelSteps.size());

            // In PlanExecutor, when launching parallel steps
            List<CompletableFuture<MultimodalResponse>> futures = new ArrayList<>();
            for (int i = 0; i < parallelSteps.size(); i++) {
                ExecutionStep step = parallelSteps.get(i);
                final int delay = i * 1000; // stagger by 1 second each
                futures.add(CompletableFuture.supplyAsync(() -> {
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    return executeStep(step, request, tracker, null);
                }, parallelExecutor).exceptionally(ex -> {
                    log.warn("[PlanExecutor] Parallel step={} failed: {}", step.getStepId(), ex.getMessage());
                    return null;
                }));
            }

            List<MultimodalResponse> parallelResults = futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .toList();

            if (parallelResults.isEmpty()) {
                log.warn("[PlanExecutor] All parallel steps failed — attempting fallback");
                MultimodalResponse fallback = executeFallback(parallelSteps.get(0),
                        request, tracker);
                if (fallback != null) {
                    parallelResults = List.of(fallback);
                    fallbackUsed = true;
                }
            }

            results.addAll(parallelResults);
            stepsExecuted += parallelResults.size();
        }


        // ── Run sequential steps ──────────────────────────────────────────────
        MultimodalResponse previousResult = results.isEmpty() ? null : results.get(results.size() - 1);

        for (ExecutionStep step : sequentialSteps) {
            log.info("[PlanExecutor] Running sequential step={} intent={} model={}",
                    step.getStepId(), step.getIntentType(), step.getModelName());
            try {
                MultimodalResponse result = executeStep(step, request, tracker, previousResult);
                results.add(result);
                previousResult = result;
                stepsExecuted++;

            } catch (Exception e) {
                log.warn("[PlanExecutor] Step={} failed: {} — attempting fallback",
                        step.getStepId(), e.getMessage());

                MultimodalResponse fallback = executeFallback(step, request, tracker);
                if (fallback != null) {
                    results.add(fallback);
                    previousResult = fallback;
                    stepsExecuted++;
                    fallbackUsed = true;
                } else {
                    log.error("[PlanExecutor] Fallback also failed for step={} — stopping", step.getStepId());
                    break;
                }
            }
        }

        // ── Merge results ─────────────────────────────────────────────────────
        PlanResult planResult = mergeResults(results, plan, stepsExecuted, fallbackUsed);

        saveMemory(request, planResult);
        log.info("[PlanExecutor] END — stepsExecuted={} fallback={} matchedIds={}",
                stepsExecuted, fallbackUsed, planResult.getMatchedIds().size());

        return MultimodalResponse.builder()
                .type(planResult.getType())
                .answer(planResult.getAnswer())
                .matchProducts(planResult.getMatchedIds())
                .language(plan.getLanguage())
                .inputType(request.getInputType())
                .suggestedOptions(List.of())
                .responseTime(java.time.LocalDateTime.now())
                .build();
    }

    private MultimodalResponse executeStep(ExecutionStep step,
                                           MultimodalRequest originalRequest,
                                           TokenTracker tracker,
                                           MultimodalResponse previousResult) {

        log.info("[PlanExecutor] Executing step={} agent={} model={}",
                step.getStepId(), step.getAgentType(), step.getModelName());

        // build request for this step
        MultimodalRequest stepRequest = buildStepRequest(step, originalRequest, previousResult);
        stepRequest.setTokenTracker(tracker);
        stepRequest.setExecutionPlan(null); // prevent re-planning

        return coordinator.process(stepRequest);
    }


    private MultimodalResponse executeFallback(ExecutionStep step,
                                               MultimodalRequest originalRequest,
                                               TokenTracker tracker) {
        String fallbackModel = nextModel(step.getModelName());
        if (fallbackModel == null) {
            log.error("[PlanExecutor] No fallback model available for step={}", step.getStepId());
            return null;
        }

        log.info("[PlanExecutor] Fallback step={} model={} → {}",
                step.getStepId(), step.getModelName(), fallbackModel);

        try {
            ExecutionStep fallbackStep = ExecutionStep.builder()
                    .stepId(step.getStepId() + "-fallback")
                    .stepName(step.getStepName() + "-fallback")
                    .intentType(step.getIntentType())
                    .complexity(step.getComplexity())
                    .agentType(step.getAgentType())
                    .modelName(fallbackModel)
                    .canRunParallel(false)
                    .dependsOnStepId(step.getDependsOnStepId())
                    .parameters(step.getParameters())
                    .build();

            return executeStep(fallbackStep, originalRequest, tracker, null);

        } catch (Exception e) {
            log.error("[PlanExecutor] Fallback failed for step={}: {}", step.getStepId(), e.getMessage());
            return null;
        }
    }

    private MultimodalRequest buildStepRequest(ExecutionStep step,
                                               MultimodalRequest original,
                                               MultimodalResponse previousResult) {
        MultimodalRequest stepRequest = new MultimodalRequest();
        stepRequest.setUserId(original.getUserId());
        stepRequest.setDetectedLanguage(original.getDetectedLanguage());
        stepRequest.setInputType(original.getInputType());
        stepRequest.setAudioBase64(original.getAudioBase64());
        stepRequest.setImageBase64(original.getImageBase64());
        stepRequest.setImageMediaType(original.getImageMediaType());
        stepRequest.setMemoryContext(original.getMemoryContext());
        stepRequest.setExecutionPlan(null);    // ← move here from executeStep
        stepRequest.setParallelStep(step.isCanRunParallel());  // ← add this

        // use step goal if available, otherwise fall back to original question
        String question = (step.getGoal() != null && !step.getGoal().isBlank())
                ? step.getGoal()
                : original.getTextQuestion();

        // if depends on previous step, append matched IDs from previous result
        if (step.getDependsOnStepId() != null && previousResult != null
                && previousResult.getMatchProducts() != null
                && !previousResult.getMatchProducts().isEmpty()) {
            question = question + " (previous results: "
                    + String.join(", ", previousResult.getMatchProducts()) + ")";
        }

        stepRequest.setTextQuestion(question);
        stepRequest.setNormalizedText(question);

        SearchIntent stepIntent = SearchIntent.builder()
                .searchType(resolveSearchTypeFromIntent(step.getIntentType()))
                .semanticQuery(question)
                .category(step.getCategory())
                .brand(step.getBrand())
                .build();

        stepRequest.setSearchIntent(stepIntent);

        return stepRequest;
    }

    // ── Merge multiple step results ───────────────────────────────────────────

    private PlanResult mergeResults(List<MultimodalResponse> results,
                                    ExecutionPlan plan,
                                    int stepsExecuted,
                                    boolean fallbackUsed) {
        if (results.isEmpty()) {
            return PlanResult.builder()
                    .answer(plan.getLanguage() == Language.ARABIC
                            ? "عذراً، لم أتمكن من معالجة طلبك."
                            : "Sorry, I could not process your request.")
                    .type("error")
                    .matchedIds(List.of())
                    .language(plan.getLanguage())
                    .clarificationRequired(false)
                    .clarificationContext(null)
                    .stepsExecuted(stepsExecuted)
                    .fallbackUsed(fallbackUsed)
                    .build();
        }

        // single step — return directly
        if (results.size() == 1) {
            MultimodalResponse r = results.get(0);
            return PlanResult.builder()
                    .answer(r.getAnswer())
                    .type(r.getType())
                    .matchedIds(r.getMatchProducts() != null ? r.getMatchProducts() : List.of())
                    .language(plan.getLanguage())
                    .clarificationRequired(false)
                    .clarificationContext(null)
                    .stepsExecuted(stepsExecuted)
                    .fallbackUsed(fallbackUsed)
                    .build();
        }

        // multi-step — if last step is comparison or knowledge it's the definitive answer
        MultimodalResponse last = results.get(results.size() - 1);
        if ("comparison".equals(last.getType()) || "knowledge".equals(last.getType())) {
            return PlanResult.builder()
                    .answer(last.getAnswer())
                    .type(last.getType())
                    .matchedIds(last.getMatchProducts() != null ? last.getMatchProducts() : List.of())
                    .language(plan.getLanguage())
                    .clarificationRequired(false)
                    .clarificationContext(null)
                    .stepsExecuted(stepsExecuted)
                    .fallbackUsed(fallbackUsed)
                    .build();
        }

        // multi-step — merge answers and matched ids
        String mergedAnswer = results.stream()
                .map(MultimodalResponse::getAnswer)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n\n"));

        List<String> mergedIds = results.stream()
                .filter(r -> r.getMatchProducts() != null)
                .flatMap(r -> r.getMatchProducts().stream())
                .distinct()
                .toList();

        String mergedType = results.stream()
                .map(MultimodalResponse::getType)
                .filter(Objects::nonNull)
                .reduce((a, b) -> a + "+" + b)
                .orElse("multi");

        return PlanResult.builder()
                .answer(mergedAnswer)
                .type(cleanType(mergedType))
                .matchedIds(mergedIds)
                .language(plan.getLanguage())
                .clarificationRequired(false)
                .clarificationContext(null)
                .stepsExecuted(stepsExecuted)
                .fallbackUsed(fallbackUsed)
                .build();
    }

    private void saveMemory(MultimodalRequest request, PlanResult planResult) {
        try {
            String[] matchedIds = planResult.getMatchedIds() != null
                    ? planResult.getMatchedIds().toArray(String[]::new)
                    : new String[0];

            // save user question
            memoryService.saveMemory(
                    request.getUserId(),
                    planResult.getType(),
                    MessageRole.USER,
                    request.getTextQuestion(),
                    matchedIds);

            // save AI answer — summarize if long
            String answer = planResult.getAnswer();
            if (answer != null && answer.length() > 300) {
                answer = answer.substring(0, 300) + "...";
            }

            memoryService.saveMemory(
                    request.getUserId(),
                    planResult.getType(),
                    MessageRole.AI,
                    answer,
                    matchedIds);

            log.info("[PlanExecutor] memory saved — userId={} type={} matchedIds={}",
                    request.getUserId(), planResult.getType(), matchedIds.length);

        } catch (Exception e) {
            log.warn("[PlanExecutor] failed to save memory: {}", e.getMessage());
        }
    }

    private String cleanType(String type) {
        if (type == null) return "unknown";
        if (type.contains("+")) return "multi";
        return type;
    }
    // ── Model fallback chain ──────────────────────────────────────────────────

    private String nextModel(String currentModel) {
        return switch (currentModel) {
            case "fast" -> "medium";
            case "medium" -> "powerful";
            case "powerful" -> null;  // no fallback — already at max
            default -> "fast";
        };
    }

    private String resolveSearchTypeFromIntent(IntentType intentType) {
        return switch (intentType) {
            case SEARCH -> "category";
            case FILTER -> "hybrid";
            case SORT -> "sort";
            case PRICE -> "price";
            case SEMANTIC, RECOMMENDATION -> "semantic";
            case COMPARISON -> "comparison";
            case KNOWLEDGE -> "knowledge";
            case SUGGESTION -> "suggest";
            default -> "category";
        };
    }
}
