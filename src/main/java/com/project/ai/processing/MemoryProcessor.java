package com.project.ai.processing;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:13 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class MemoryProcessor implements ChatProcessor {

    private final MemoryService memoryService;
    private final IntentAnalyzer intentAnalyzer;

    @Override
    public boolean supports(String searchType) {
        return false;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        throw new UnsupportedOperationException("MemoryProcessor is used as pre/post step only");
    }

    public void prepareContext(ProcessingRequest request) {
        log.info("[MemoryProcessor] prepareContext START — userId={}", request.getUserId());

        String memoryContext = memoryService.memoryContext(
                request.getUserId(), request.getRawQuestion());

        log.debug("[MemoryProcessor] Memory context loaded:\n{}", memoryContext);

        String enriched = intentAnalyzer.enrichWithMemory(
                request.getRawQuestion(), memoryContext);

        request.setMemoryContext(memoryContext);
        request.setEnrichedQuestion(enriched);

        log.info("[MemoryProcessor] Question enriched — from='{}' to='{}'",
                request.getRawQuestion(), enriched);
    }


    public void saveToMemory(ProcessingRequest request, ProcessingResult result) {

        log.info("[MemoryProcessor] saveToMemory START — userId={}, type={}",
                request.getUserId(), result.getType());

        String[] matchedProducts = result.getMatchedIds().toArray(String[]::new);

        String cleanAnswer = result.getAnswer()
                .replaceAll("(?m)^List all \\d+ products:.*$", "")
                .replaceAll("(?m)^Format:.*$", "")
                .replaceAll("(?m)^Product Name.*$", "")
                .trim();



        log.debug("[MemoryProcessor] Saving USER message='{}'", request.getUserId());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts);

        log.debug("[MemoryProcessor] Saving AI answer='{}'", result.getAnswer());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                cleanAnswer,
                matchedProducts);

        log.debug("[MemoryProcessor] Saving matchedProducts={}", result.getMatchedIds());

        log.info("[MemoryProcessor] saveToMemory END");
    }
}
