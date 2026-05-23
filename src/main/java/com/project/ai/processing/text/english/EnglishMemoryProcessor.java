package com.project.ai.processing.text.english;

import com.project.ai.dto.AiResult;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.MemoryContext;
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
public class EnglishMemoryProcessor implements ChatProcessor, MemoryContext {

    private final MemoryService memoryService;
    private final EnglishIntentAnalyzer englishIntentAnalyzer;

    @Override
    public boolean supports(String searchType) {
        return false;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        throw new UnsupportedOperationException("EnglishMemoryProcessor is used as pre/post step only");
    }

    @Override
    public void prepareContext(final ProcessingRequest request) {
        log.info("[EnglishMemoryProcessor] prepareContext START — userId={}", request.getUserId());

        String memoryContext = memoryService.memoryContext(
                request.getUserId(), request.getRawQuestion());

        log.debug("[EnglishMemoryProcessor] Memory context loaded:\n{}", memoryContext);

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        AiResult<String> enriched = englishIntentAnalyzer.enrichWithMemory(
                request.getRawQuestion(), memoryContext);

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "english-memory-processor",
                enriched.inputTokens(),
                enriched.outputTokens(),
                intentDuration
        );

        request.setMemoryContext(memoryContext);
        request.setEnrichedQuestion(enriched.result());

        log.info("[EnglishMemoryProcessor] Question enriched — from='{}' to='{}'",
                request.getRawQuestion(), enriched.result());
    }

    @Override
    public void saveToMemory(final ProcessingRequest request, ProcessingResult result) {

        log.info("[EnglishMemoryProcessor] saveToMemory START — userId={}, type={}",
                request.getUserId(), result.getType());

        String[] matchedProducts = result.getMatchedIds().toArray(String[]::new);

        String cleanAnswer = result.getAnswer()
                .replaceAll("(?m)^List all \\d+ products:.*$", "")
                .replaceAll("(?m)^Format:.*$", "")
                .replaceAll("(?m)^Product Name.*$", "")
                .trim();


        log.debug("[EnglishMemoryProcessor] Saving USER message='{}'", request.getUserId());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts);

        log.debug("[EnglishMemoryProcessor] Saving AI answer='{}'", result.getAnswer());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                cleanAnswer,
                matchedProducts);

        log.debug("[EnglishMemoryProcessor] Saving matchedProducts={}", result.getMatchedIds());

        log.info("[EnglishMemoryProcessor] saveToMemory END");
    }
}
