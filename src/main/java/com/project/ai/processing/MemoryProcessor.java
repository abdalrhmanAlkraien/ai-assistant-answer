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
        String memoryContext = memoryService.memoryContext(
                request.getUserId(), request.getRawQuestion());

        String enriched = intentAnalyzer.enrichWithMemory(
                request.getRawQuestion(), memoryContext);

        request.setMemoryContext(memoryContext);
        request.setEnrichedQuestion(enriched);

        log.info("Memory context loaded. Enriched question: {}", enriched);
    }


    public void saveToMemory(ProcessingRequest request, ProcessingResult result) {
        String[] matchedProducts = result.getMatchedIds().toArray(String[]::new);

        String cleanAnswer = result.getAnswer()
                .replaceAll("(?m)^List all \\d+ products:.*$", "")
                .replaceAll("(?m)^Format:.*$", "")
                .replaceAll("(?m)^Product Name.*$", "")
                .trim();

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts);

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                cleanAnswer,
                matchedProducts);
    }
}
