package com.project.ai.service;

import com.project.ai.config.EvalsProperties;
import com.project.ai.dto.evels.ContextDetailDto;
import com.project.ai.dto.evels.ContextDto;
import com.project.ai.dto.evels.ContextPageDto;
import com.project.ai.dto.evels.EvalPollResponse;
import com.project.ai.dto.evels.EvalTriggerRequest;
import com.project.ai.dto.evels.EvalTriggerResponse;
import com.project.ai.dto.evels.EvaluationDetailDto;
import com.project.ai.dto.evels.EvaluationPageDto;
import com.project.ai.dto.evels.EvaluationSummaryDto;
import com.project.ai.dto.evels.EvaluationTypeDto;
import com.project.ai.dto.evels.FailureDto;
import com.project.ai.model.evels.Context;
import com.project.ai.model.evels.Evaluation;
import com.project.ai.model.evels.EvaluationType;
import com.project.ai.repository.ContextRepository;
import com.project.ai.repository.EvaluationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:41 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final ContextRepository contextRepository;

    private final EvalsProperties evalsProperties;
    private final RestTemplate restTemplate;

    @Transactional(readOnly = true)
    public Page<EvaluationPageDto> getEvaluations(
            String status, String triggeredType, String language, Pageable pageable) {

        // map frontend filter values to DB values
        String dbResultType = mapTriggeredType(triggeredType);
        String dbLang       = mapLanguage(language);
        String dbStatus     = mapStatus(status);

        return evaluationRepository
                .findAllWithFilters(dbStatus, dbResultType, dbLang, pageable)
                .map(this::toPageDto);
    }

    @Transactional(readOnly = true)
    public EvaluationDetailDto getEvaluationById(Long id) {
        Evaluation eval = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evaluation not found: " + id));

        // initialize lazily inside the transaction
        Hibernate.initialize(eval.getEvaluationTypes());
        eval.getEvaluationTypes().forEach(t -> Hibernate.initialize(t.getContexts()));
        Hibernate.initialize(eval.getFailures());

        return toDetailDto(eval);
    }

    @Transactional(readOnly = true)
    public EvaluationSummaryDto getLastEvaluationSummary() {
        return evaluationRepository.findTopByOrderByCreatedAtDesc()
                .map(this::toSummaryDto)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No evaluations found"));
    }

    @Transactional(readOnly = true)
    public Page<ContextPageDto> getContexts(
            Long evalId, String language, String status,
            String searchType, Pageable pageable) {

        if (!evaluationRepository.existsById(evalId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Evaluation not found: " + evalId);
        }

        // map frontend filter value to DB value
        String dbStatus = null;
        if (status != null) {
            dbStatus = switch (status.toUpperCase()) {
                case "PASS"    -> "SUCCESS";
                case "FAIL"    -> "FAILED";
                default        -> status;
            };
        }

        AtomicInteger counter = new AtomicInteger((int) pageable.getOffset() + 1);

        return contextRepository
                .findByEvaluationIdWithFilters(evalId, language, dbStatus, searchType, pageable)
                .map(c -> toContextPageDto(c, counter.getAndIncrement()));
    }

    @Transactional(readOnly = true)
    public ContextDetailDto getContextDetail(Long evalId, Long contextId) {
        Context c = contextRepository
                .findByIdAndEvaluationTypeEvaluationId(contextId, evalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Context not found: " + contextId));
        return toContextDetailDto(c);
    }

    public Map<String, Object> isRunning() {
        Optional<Evaluation> running = evaluationRepository
                .findTopByStatusOrderByCreatedAtDesc("in_progress");

        Map<String, Object> response = new HashMap<>();

        if (running.isEmpty()) {
            response.put("running", false);
            response.put("evaluation", null);
            return response;
        }

        Evaluation eval = running.get();

        Map<String, Object> evalDetails = new HashMap<>();
        evalDetails.put("id",             eval.getId());
        evalDetails.put("runId",          eval.getRunId());
        evalDetails.put("startedAt",      eval.getCreatedAt());
        evalDetails.put("totalEvaluated", eval.getTotalEvaluated());

        response.put("running",    true);
        response.put("evaluation", evalDetails);

        return response;
    }

    public EvalTriggerResponse triggerAsync(EvalTriggerRequest request) {

        if (!evalsProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Running evals is a separate add-on. Contact MIGFORA to enable.");
        }

        try {
            log.info("[EvalTriggerService] triggering async eval lang='{}' searchType='{}'",
                    request.getLang(), request.getSearchType());

            // map to Python request format
            Map<String, Object> pythonRequest = new HashMap<>();
            pythonRequest.put("lang",        request.getLang());
            pythonRequest.put("search_type", request.getSearchType());
            pythonRequest.put("limit",       request.getLimit());
            pythonRequest.put("save_report", request.getSaveReport());

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    evalsProperties.getEvelsUrl() + "/api/v1/eval/run/async",
                    pythonRequest,
                    Map.class
            );

            Map body = response.getBody();
            log.info("[EvalTriggerService] eval triggered — evalId='{}'",
                    body.get("eval_id"));

            return EvalTriggerResponse.builder()
                    .evalId((Integer) body.get("eval_id"))
                    .status((String) body.get("status"))
                    .lang(request.getLang())
                    .searchType(request.getSearchType())
                    .totalCases((Integer) body.get("total_cases"))
                    .build();

        } catch (Exception e) {
            log.error("[EvalTriggerService] failed to trigger eval: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to trigger eval: " + e.getMessage());
        }
    }

    public EvalPollResponse pollEval(Integer evalId) {
        try {
            log.info("[EvalTriggerService] polling eval evalId='{}'", evalId);

            ResponseEntity<Map> response = restTemplate.getForEntity(
                    evalsProperties.getEvelsUrl() + "/api/v1/eval/runs/" + evalId,
                    Map.class
            );

            Map body = response.getBody();

            return EvalPollResponse.builder()
                    .evalId((Integer) body.get("eval_id"))
                    .status((String) body.get("status"))
                    .passed((Boolean) body.get("passed"))
                    .totalEvaluated((Integer) body.get("total_evaluated"))
                    .totalSkipped((Integer) body.get("total_skipped"))
                    .lang((List<String>) body.get("lang"))
                    .resultType((String) body.get("result_type"))
                    .hasInconclusive((Boolean) body.get("has_inconclusive"))
                    .createdAt((String) body.get("created_at"))
                    .completedAt((String) body.get("completed_at"))
                    .reportUrl((String) body.get("report_url"))
                    .casesCompleted((Integer) body.get("cases_completed"))
                    .casesTotal((Integer) body.get("cases_total"))
                    .progressPct(body.get("progress_pct") != null
                            ? ((Number) body.get("progress_pct")).doubleValue()
                            : null)
                    .build();

        } catch (Exception e) {
            log.error("[EvalTriggerService] failed to poll eval: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to poll eval: " + e.getMessage());
        }
    }

    // ---------- Mapper -------------
    private EvaluationPageDto toPageDto(Evaluation e) {
        return EvaluationPageDto.builder()
                .id(e.getId())
                .runId(e.getRunId())
                .status(mapStatusDisplay(e.getStatus()))
                .triggeredType(mapResultTypeDisplay(e.getResultType()))
                .language(mapLangDisplay(e.getLang()))
                .totalQueries(e.getTotalEvaluated())
                .runAt(e.getCreatedAt())
                .passed(e.getPassed())
                .build();
    }

    private EvaluationDetailDto toDetailDto(Evaluation e) {
        List<EvaluationTypeDto> types = e.getEvaluationTypes().stream()
                .map(this::toTypeDto)
                .toList();

        List<FailureDto> failures = e.getFailures().stream()
                .map(f -> FailureDto.builder()
                        .id(f.getId())
                        .lang(f.getLang())
                        .searchType(f.getSearchType())
                        .metric(f.getMetric())
                        .score(f.getScore())
                        .threshold(f.getThreshold())
                        .humanReadable(f.getHumanReadable())
                        .build())
                .toList();

        return EvaluationDetailDto.builder()
                .id(e.getId())
                .runId(e.getRunId())
                .status(mapStatusDisplay(e.getStatus()))
                .triggeredType(mapResultTypeDisplay(e.getResultType()))
                .language(mapLangDisplay(e.getLang()))
                .totalEvaluated(e.getTotalEvaluated())
                .totalSkipped(e.getTotalSkipped())
                .passed(e.getPassed())
                .runAt(e.getCreatedAt())
                .completedAt(e.getCompletedAt())
                .types(types)
                .failures(failures)
                .reportStatus(e.getReportStatus())
                .reportUrl(e.getReportUrl())
                .build();
    }

    private EvaluationTypeDto toTypeDto(EvaluationType t) {
        List<ContextDto> contexts = t.getContexts().stream()
                .map(c -> ContextDto.builder()
                        .id(c.getId())
                        .question(c.getQuestion())
                        .language(c.getLanguage())
                        .searchType(c.getSearchType())
                        .expectedSearchType(c.getExpectedSearchType())
                        .returnedSearchType(c.getReturnedSearchType())
                        .correctType(c.getCorrectType())
                        .latencyMs(c.getLatencyMs())
                        .status(c.getStatus())
                        .passed(c.getPassed())
                        .answer(c.getAnswer())
                        .groundTruth(c.getGroundTruth())
                        .contexts(c.getContexts())
                        .matchedProductIds(c.getMatchedProductIds())
                        .productCount(c.getProductCount())
                        .expectedProductCount(c.getExpectedProductCount())
                        .llmJudge(c.getLlmJudge())
                        .noError(c.getNoError())
                        .build())
                .toList();

        return EvaluationTypeDto.builder()
                .id(t.getId())
                .lang(t.getLang())
                .searchType(t.getSearchType())
                .resultType(t.getResultType())
                .passed(t.getPassed())
                .passRate(t.getPassRate())
                .passedCases(t.getPassedCases())
                .totalCases(t.getTotalCases())
                .correctTypeRate(t.getCorrectTypeRate())
                .rightProductsReturned(t.getRightProductsReturned())
                .noMissingProducts(t.getNoMissingProducts())
                .noHallucination(t.getNoHallucination())
                .clarificationPassRate(t.getClarificationPassRate())
                .safeResponseRate(t.getSafeResponseRate())
                .contexts(contexts)
                .build();
    }

    private EvaluationSummaryDto toSummaryDto(Evaluation e) {
        return EvaluationSummaryDto.builder()
                .lastRun(e.getCreatedAt())
                .status(mapStatusDisplay(e.getStatus()))
                .triggeredType(mapResultTypeDisplay(e.getResultType()))
                .language(mapLangDisplay(e.getLang()))
                .totalQueries(e.getTotalEvaluated())
                .passed(e.getPassed())
                .reportStatus(e.getReportStatus())
                .reportUrl(e.getReportUrl())
                .build();
    }

    // ── Filter value mappers (frontend → DB) ──────────────────────────────────

    private String mapStatus(String status) {
        if (status == null || status.equalsIgnoreCase("all")) return null;
        return switch (status.toLowerCase()) {
            case "completed" -> "completed";
            case "running"   -> "in_progress";
            case "failed"    -> "failed";
            default          -> null;
        };
    }

    private String mapTriggeredType(String triggeredType) {
        if (triggeredType == null || triggeredType.equalsIgnoreCase("all")) return null;
        return switch (triggeredType.toLowerCase()) {
            case "specific" -> "specific";
            case "all"      -> "all";
            default         -> null;
        };
    }

    private String mapLanguage(String language) {
        if (language == null || language.equalsIgnoreCase("all")) return null;
        return switch (language.toLowerCase()) {
            case "arabic"  -> "arabic";
            case "english" -> "english";
            default        -> null;
        };
    }

    // ── Display value mappers (DB → frontend) ─────────────────────────────────

    private String mapStatusDisplay(String status) {
        return switch (status) {
            case "in_progress" -> "Running";
            case "completed"   -> "Completed";
            case "failed"      -> "Failed";
            default            -> status;
        };
    }

    private String mapResultTypeDisplay(String resultType) {
        if (resultType == null) return "All";
        return switch (resultType.toLowerCase()) {
            case "all"      -> "All";
            case "specific" -> "Specific";
            default         -> resultType;
        };
    }

    private String mapLangDisplay(String[] lang) {
        if (lang == null || lang.length == 0) return "All";
        if (lang.length == 1) {
            return switch (lang[0].toLowerCase()) {
                case "arabic"  -> "Arabic";
                case "english" -> "English";
                default        -> lang[0];
            };
        }
        return "All";
    }

    private ContextPageDto toContextPageDto(Context c, int index) {
        return ContextPageDto.builder()
                .id(c.getId())
                .index(index)
                .question(c.getQuestion())
                .searchType(c.getSearchType())
                .expectedSearchType(c.getExpectedSearchType())
                .returnedSearchType(c.getReturnedSearchType())
                .language(c.getLanguage() != null
                        ? c.getLanguage().toUpperCase() : null)
                .latencyMs(c.getLatencyMs())
                .status(mapContextStatus(c))   // ← use mapper
                .error(resolveError(c))
                .build();
    }

    private String mapContextStatus(Context c) {
        if (c.getPassed() != null) {
            return c.getPassed() ? "PASS" : "FAIL";
        }
        if (c.getStatus() != null) {
            return switch (c.getStatus().toUpperCase()) {
                case "SUCCESS" -> "PASS";
                case "FAILED", "FAILURE" -> "FAIL";
                default -> c.getStatus().toUpperCase();
            };
        }
        return null;
    }

    private ContextDetailDto toContextDetailDto(Context c) {
        return ContextDetailDto.builder()
                .id(c.getId())
                .question(c.getQuestion())
                .language(c.getLanguage())
                .searchType(c.getSearchType())
                .expectedSearchType(c.getExpectedSearchType())
                .returnedSearchType(c.getReturnedSearchType())
                .correctType(c.getCorrectType())
                .latencyMs(c.getLatencyMs())
                .status(c.getStatus())
                .passed(c.getPassed())
                .answer(c.getAnswer())
                .groundTruth(c.getGroundTruth())
                .contexts(c.getContexts())
                .matchedProductIds(c.getMatchedProductIds())
                .productCount(c.getProductCount())
                .expectedProductCount(c.getExpectedProductCount())
                .llmJudge(c.getLlmJudge())
                .noError(c.getNoError())
                .error(resolveError(c))
                .build();
    }

    private String resolveError(Context c) {
        if (Boolean.FALSE.equals(c.getCorrectType())) return "Type mismatch";
        if (Boolean.FALSE.equals(c.getNoError()))     return "Error in response";
        return null;
    }
}
