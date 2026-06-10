package com.project.ai.processing.text.english;

import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.AiResult;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
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
 * @Date: 12/05/2026
 * @Time: 9:13 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class EnglishMemoryProcessor implements ChatProcessor, MemoryContext {

    private final MemoryService memoryService;
    @Qualifier("englishChatModel")
    private final ChatModel chatModel;   // inject fastChatModel

    @Override
    public boolean supports(String searchType) {
        return false;
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {
        throw new UnsupportedOperationException("EnglishMemoryProcessor is used as pre/post step only");
    }

    @Override
    public void saveToMemory(final ProcessingRequest request, ProcessingResult result) {

        log.info("[EnglishMemoryProcessor] saveToMemory START — userId={}, type={}",
                request.getUserId(), result.getType());

        String[] matchedProducts = result.getMatchedIds().toArray(String[]::new);

        String cleanAnswer = result.getAnswer() != null
                ? result.getAnswer()
                .replaceAll("(?m)^List all \\d+ products:.*$", "")
                .replaceAll("(?m)^Format:.*$", "")
                .replaceAll("(?m)^Product Name.*$", "")
                .trim()
                : "";


        log.debug("[EnglishMemoryProcessor] Saving USER message='{}'", request.getUserId());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.USER,
                request.getSearchIntent().getSemanticQuery(),
                matchedProducts,
                Language.ENGLISH);

        String summarized = summarizeIfNeeded(cleanAnswer, result.getType());

        log.debug("[EnglishMemoryProcessor] Saving AI answer='{}'", result.getAnswer());

        memoryService.saveMemory(
                request.getUserId(),
                request.getSearchIntent(),
                com.project.ai.model.MessageRole.AI,
                summarized,
                matchedProducts,
                Language.ENGLISH);

        log.debug("[EnglishMemoryProcessor] Saving matchedProducts={}", result.getMatchedIds());

        log.info("[EnglishMemoryProcessor] saveToMemory END");
    }


    private String summarizeIfNeeded(String answer, String type) {
        if (answer == null || answer.isBlank()) return "";

        // listing types — already short, no summarization needed
        if (Set.of("category", "brand", "price", "hybrid", "sort").contains(type)) {
            return answer.length() > 300
                    ? answer.substring(0, 300) + "..."
                    : answer;
        }

        // knowledge, semantic, comparison — may be verbose, summarize
        if (answer.length() > 300) {
            log.info("[EnglishMemoryProcessor] summarizing answer length={} type={}",
                    answer.length(), type);
            return summarize(answer);
        }

        return answer;
    }

    private String summarize(String answer) {
        try {
            String prompt = """
                Summarize this in 1-2 sentences, keeping key facts only.
                Return ONLY the summary, nothing else.
                
                Text: %s
                """.formatted(answer);

            String summary = chatModel.chat(prompt).trim();
            log.info("[EnglishMemoryProcessor] summarized {} → {} chars",
                    answer.length(), summary.length());
            return summary;

        } catch (Exception e) {
            log.warn("[EnglishMemoryProcessor] summarization failed — truncating: {}", e.getMessage());
            return answer.substring(0, 300) + "...";
        }
    }
}
