package com.project.ai.processing.text.arabic;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.AiResult;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import com.project.ai.processing.text.structure.MemoryContext;
import com.project.ai.service.MemoryService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:11 PM
 */
@Service
@Log4j2
public class ArabicMemoryProcessor implements ChatProcessor, MemoryContext {

    private final MemoryService memoryService;
    private final IntentAnalyzer arabicIntentAnalyzer;
    private final LangChain4jProperties properties;

    public ArabicMemoryProcessor(
            final MemoryService memoryService,
            final ArabicIntentAnalyzer arabicIntentAnalyzer,
            final LangChain4jProperties properties) {

        this.memoryService = memoryService;
        this.arabicIntentAnalyzer = arabicIntentAnalyzer;
        this.properties = properties;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        throw new UnsupportedOperationException("EnglishMemoryProcessor is used as pre/post step only");
    }

    @Override
    public boolean supports(String searchType) {
        return false;
    }

    @Override
    public void prepareContext(ProcessingRequest request) {

        log.info("[ArabicMemoryProcessor] prepareContext START — userId={}", request.getUserId());

        String memoryContext = memoryService.memoryContext(
                request.getUserId(), request.getRawQuestion());

        log.debug("[ArabicMemoryProcessor] Memory context:\n{}", memoryContext);

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        AiResult<String> answer = arabicIntentAnalyzer.enrichWithMemory(
                request.getRawQuestion(), memoryContext);

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "arabic-memory-processor",
                properties.getChatModel().getOllama().getArabicModelName(),
                answer.inputTokens(),
                answer.outputTokens(),
                intentDuration
        );

        log.info("[ArabicMemoryProcessor] enriched: '{}' → '{}'",
                request.getRawQuestion(), answer.result());

        request.setMemoryContext(memoryContext);
        request.setEnrichedQuestion(answer.result().trim());
    }

    @Override
    public void saveToMemory(final ProcessingRequest request, ProcessingResult result) {

        log.info("[ArabicMemoryProcessor] saveToMemory START — userId={}, type={}",
                request.getUserId(), result.getType());

        String[] matchedProducts = result.getMatchedIds().toArray(String[]::new);

        String cleanAnswer = result.getAnswer()
                .replaceAll("(?m)^List all \\d+ products:.*$", "")
                .replaceAll("(?m)^Format:.*$", "")
                .replaceAll("(?m)^Product Name.*$", "")
                .trim();


        log.debug("[ArabicMemoryProcessor] Saving USER message='{}'", request.getUserId());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts);

        log.debug("[ArabicMemoryProcessor] Saving AI answer='{}'", result.getAnswer());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                cleanAnswer,
                matchedProducts);

        log.debug("[MemoryProcessor] Saving matchedProducts={}", result.getMatchedIds());

        log.info("[ArabicMemoryProcessor] saveToMemory END");
    }
}
