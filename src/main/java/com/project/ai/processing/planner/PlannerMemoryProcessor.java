package com.project.ai.processing.planner;

import com.project.ai.dto.MultimodalRequest;
import com.project.ai.model.planner.PlannerMemoryResult;
import com.project.ai.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 24/05/2026
 * @Time: 12:24 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class PlannerMemoryProcessor {

    private final MemoryService          memoryService;
    private final PlannerMemoryEnricher  enricher;

    public PlannerMemoryResult prepare(MultimodalRequest request) {
        log.info("[PlannerMemoryProcessor] START — userId={} language={}", request.getUserId(), request.getDetectedLanguage());

        // ── Fetch raw memory ──────────────────────────────────────────────────
        String memoryContext = null;
        try {
            memoryContext = memoryService.memoryContext(request.getUserId(), request.getTextQuestion());
        } catch (Exception e) {
            log.warn("[PlannerMemoryProcessor] Failed to load memory: {}", e.getMessage());
        }

        if (memoryContext == null || memoryContext.isBlank()) {
            log.info("[PlannerMemoryProcessor] No memory — returning original question");
            return new PlannerMemoryResult(null, request.getTextQuestion());
        }

        log.debug("[PlannerMemoryProcessor] Memory context:\n{}", memoryContext);

        // ── Enrich question with memory ───────────────────────────────────────
        String enrichResult = enricher.enrich(request, memoryContext);

        log.info("[PlannerMemoryProcessor] END — enriched='{}'", enrichResult);

        return new PlannerMemoryResult(memoryContext, enrichResult);
    }

    public String getMemoryContext(MultimodalRequest request) {
        log.info("[PlannerMemoryProcessor] START getMemoryContext — userId={} language={}", request.getUserId(), request.getDetectedLanguage());

        // ── Fetch raw memory ──────────────────────────────────────────────────
        String memoryContext = null;
        try {
            memoryContext = memoryService.memoryContext(request.getUserId(), request.getTextQuestion());
        } catch (Exception e) {
            log.warn("[PlannerMemoryProcessor] getMemoryContext — Failed to load memory: {}", e.getMessage());
        }

        if (memoryContext == null || memoryContext.isBlank()) return "";

        // truncate long memory for analyzer — keep last 500 chars max
        if (memoryContext.length() > 500) {
            log.info("[PlannerMemoryProcessor] truncating memory {} → 500 chars",
                    memoryContext.length());
            return memoryContext.substring(memoryContext.length() - 500);
        }

        return memoryContext;
    }
}
