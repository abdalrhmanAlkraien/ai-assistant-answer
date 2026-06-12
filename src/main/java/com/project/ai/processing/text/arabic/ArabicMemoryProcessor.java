package com.project.ai.processing.text.arabic;

import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.model.MessageRole;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import com.project.ai.processing.text.structure.MemoryContext;
import com.project.ai.service.MemoryService;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:11 PM
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class ArabicMemoryProcessor implements ChatProcessor, MemoryContext {

    private final MemoryService memoryService;
    @Qualifier("arabicChatModel")
    private final ChatModel chatModel;   // inject fastChatModel


    @Override
    public ProcessingResult process(ProcessingRequest request) {
        throw new UnsupportedOperationException("EnglishMemoryProcessor is used as pre/post step only");
    }

    @Override
    public boolean supports(String searchType) {
        return false;
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
                MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts,
                Language.ARABIC);

        log.debug("[ArabicMemoryProcessor] Saving AI answer='{}'", result.getAnswer());

        String summarized = summarizeIfNeeded(cleanAnswer, result.getType());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                summarized,
                matchedProducts,
                Language.ARABIC);

        log.debug("[ArabicMemoryProcessor] Saving matchedProducts={}", result.getMatchedIds());

        log.info("[ArabicMemoryProcessor] saveToMemory END");
    }

    private String summarizeIfNeeded(String answer, String type) {
        if (answer == null || answer.isBlank()) return "";

        // listing types — truncate only
        if (Set.of("category", "brand", "price", "hybrid", "sort").contains(type)) {
            return answer.length() > 500
                    ? answer.substring(0, 500) + "..."
                    : answer;
        }

        // semantic, comparison, knowledge — truncate at 800, no LLM call
        return answer.length() > 800
                ? answer.substring(0, 800) + "..."
                : answer;
    }

    private String summarize(String answer) {
        try {
            String prompt = """
                Summarize this in 1-2 sentences, keeping key facts only.
                Return ONLY the summary, nothing else.
                
                Text: %s
                """.formatted(answer);

            String summary = chatModel.chat(prompt).trim();
            log.info("[ArabicMemoryProcessor] summarized {} → {} chars",
                    answer.length(), summary.length());
            return summary;

        } catch (Exception e) {
            log.warn("[ArabicMemoryProcessor] summarization failed — truncating: {}", e.getMessage());
            return answer.substring(0, 300) + "...";
        }
    }
}
