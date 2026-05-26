package com.project.ai.strategy.ecommerce.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.planner.AgentType;
import com.project.ai.model.planner.ClarificationContext;
import com.project.ai.model.planner.ComplexityLevel;
import com.project.ai.model.planner.EcommerceStoreContext;
import com.project.ai.model.planner.ExecutionPlan;
import com.project.ai.model.planner.ExecutionStep;
import com.project.ai.model.planner.IntentType;
import com.project.ai.model.planner.RequestAnalysis;
import com.project.ai.processing.planner.ExecutionPlanner;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 11:13 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EcommerceExecutionPlanner implements ExecutionPlanner {

    private final EcommerceContextBuilder ecommerceContextBuilder;
    @Qualifier("analyzerChatModel")
    private final ChatModel fastChatModel;
    private final LangChain4jProperties properties;
    private final PromptLoader promptLoader;



    @Override
    public ExecutionPlan plan(MultimodalRequest request, String enrichedQuestion, RequestAnalysis analysis) {

        log.info("[ExecutionPlanner] START — question='{}' complexity={} multiStep={} ambiguous={}",
                enrichedQuestion,
                analysis.getComplexity(),
                analysis.isMultiStep(),
                analysis.isAmbiguous());

        // if already analyzed as ambiguous — skip LLM call, go straight to clarification
        if (analysis.isAmbiguous()) {
            log.info("[ExecutionPlanner] Analysis flagged ambiguous — skipping LLM plan call");
            return ExecutionPlan.builder()
                    .steps(List.of())
                    .overallComplexity(analysis.getComplexity())
                    .language(request.getDetectedLanguage())
                    .requiresClarification(true)
                    .clarificationContext(null) // will be filled by AmbiguityResolver in PlanExecutor
                    .primaryModel("fast")
                    .build();
        }

        EcommerceStoreContext context = ecommerceContextBuilder.build();
        TokenTracker tracker = request.getTokenTracker();

        String promptTemplate = promptLoader.get(PromptKeys.EXECUTION_PLANNER);
        String prompt = promptTemplate.formatted(
                String.join(", ", context.getAvailableCategories()),
                String.join(", ", context.getAvailableBrands()),
                context.getMinPrice(),
                context.getMaxPrice(),
                request.getDetectedLanguage().name(),
                enrichedQuestion,
                analysis.getComplexity().name(),
                analysis.getIntentTypes().toString(),
                analysis.isMultiStep()
        );

        long start = System.currentTimeMillis();
        ChatResponse response = fastChatModel.chat(UserMessage.from(prompt));
        long duration = System.currentTimeMillis() - start;

        tracker.record(
                "execution-planner",
                properties.getChatModel().getModels().get("analyzer").getModelName(),
                response.tokenUsage().inputTokenCount(),
                response.tokenUsage().outputTokenCount(),
                duration
        );


        String rawText = response.aiMessage().text();
        if (rawText == null || rawText.isBlank()) {
            log.warn("[ExecutionPlanner] null response — using fallback");
            return fallbackPlan(enrichedQuestion, request.getDetectedLanguage());
        }

        String json = rawText
                .replaceAll("```json", "")
                .replaceAll("```", "")
                .trim();

        try {
            ExecutionPlan plan = parsePlan(json, request.getDetectedLanguage());

            log.info("[ExecutionPlanner] Plan built — steps={} clarification={} parallel={}",
                    plan.getSteps().size(),
                    plan.isRequiresClarification(),
                    plan.getSteps().stream().filter(ExecutionStep::isCanRunParallel).count());

            return plan;

        } catch (Exception e) {
            log.warn("[ExecutionPlanner] Failed to parse plan, using fallback: {}", e.getMessage());
            return fallbackPlan(enrichedQuestion, request.getDetectedLanguage());
        }
    }


    private ExecutionPlan parsePlan(String json, Language language) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(json);

        boolean requiresClarification = node.get("requiresClarification").asBoolean();
        String clarificationQuestion = node.get("clarificationQuestion").isNull()
                ? null : node.get("clarificationQuestion").asText();

        List<String> suggestedOptions = new ArrayList<>();
        node.get("suggestedOptions").forEach(o -> suggestedOptions.add(o.asText()));

        List<ExecutionStep> steps = new ArrayList<>();
        node.get("steps").forEach(s -> steps.add(ExecutionStep.builder()
                .stepId(s.get("stepId").asText())
                .stepName(s.get("stepName").asText())
                .goal(s.has("goal") && !s.get("goal").isNull() ? s.get("goal").asText() : null)
                .intentType(IntentType.valueOf(s.get("intentType").asText()))
                .complexity(ComplexityLevel.valueOf(s.get("complexity").asText()))
                .agentType(AgentType.valueOf(s.get("agentType").asText()))
                .modelName(s.get("modelKey").asText())
                .canRunParallel(s.get("canRunParallel").asBoolean())
                .dependsOnStepId(s.get("dependsOnStepId").isNull()
                        ? null : s.get("dependsOnStepId").asText())
                .parameters(new HashMap<>())
                .category(getTextOrNull(s, "category"))   // ← add
                .brand(getTextOrNull(s, "brand"))          // ← add
                .build()));

        ClarificationContext clarificationContext = requiresClarification
                ? ClarificationContext.builder()
                .originalQuestion(null)
                .clarificationQuestion(clarificationQuestion)
                .suggestedOptions(suggestedOptions)
                .language(language)
                .build()
                : null;

        String primaryModel = steps.isEmpty() ? "fast"
                : steps.stream()
                .map(ExecutionStep::getModelName)
                .reduce((a, b) -> rankModel(a) >= rankModel(b) ? a : b)
                .orElse("fast");

        ComplexityLevel overallComplexity = steps.isEmpty() ? ComplexityLevel.SIMPLE
                : steps.stream()
                .map(ExecutionStep::getComplexity)
                .reduce((a, b) -> a.ordinal() >= b.ordinal() ? a : b)
                .orElse(ComplexityLevel.SIMPLE);

        return ExecutionPlan.builder()
                .steps(steps)
                .overallComplexity(overallComplexity)
                .language(language)
                .requiresClarification(requiresClarification)
                .clarificationContext(clarificationContext)
                .primaryModel(primaryModel)
                .build();
    }

    // ── Fallback ──────────────────────────────────────────────────────────────

    private ExecutionPlan fallbackPlan(String question, Language language) {
        log.warn("[ExecutionPlanner] Using fallback plan for question='{}'", question);

        ExecutionStep fallbackStep = ExecutionStep.builder()
                .stepId("step-1")
                .stepName("fallback-search")
                .intentType(IntentType.SEARCH)
                .complexity(ComplexityLevel.SIMPLE)
                .agentType(language == Language.ARABIC ? AgentType.ARABIC : AgentType.ENGLISH)
                .modelName("fast")
                .canRunParallel(false)
                .dependsOnStepId(null)
                .parameters(new HashMap<>())
                .build();

        return ExecutionPlan.builder()
                .steps(List.of(fallbackStep))
                .overallComplexity(ComplexityLevel.SIMPLE)
                .language(language)
                .requiresClarification(false)
                .clarificationContext(null)
                .primaryModel("fast")
                .build();
    }

    private int rankModel(String modelKey) {
        return switch (modelKey) {
            case "fast" -> 1;
            case "medium" -> 2;
            case "powerful" -> 3;
            default -> 0;
        };
    }

    private String getTextOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private Double getDoubleOrNull(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asDouble();
    }
}
