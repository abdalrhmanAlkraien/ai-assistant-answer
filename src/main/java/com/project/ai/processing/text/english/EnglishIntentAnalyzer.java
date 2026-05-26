package com.project.ai.processing.text.english;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.AiResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import com.project.ai.util.TokenUtil;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:08 PM
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class EnglishIntentAnalyzer implements IntentAnalyzer {

    @Qualifier("englishChatModel")
    private final ChatModel chatModel;
    private final ObjectMapper mapper;
    private final PromptLoader promptLoader;

    @Override
    public AiResult<SearchIntent> extractIntent(String userQuestion) {


        log.debug("[EnglishIntentAnalyzer] Raw intent JSON:\n{}", userQuestion);

        boolean ignorePriceHint = userQuestion.toLowerCase().matches(
                ".*\\b(without.*price|ignore.*price|no.*budget|any.*price|" +
                        "regardless.*price|price.*matter|don.t care.*price|" +
                        "care about.*price|without care)\\b.*");

        String priceInstruction = ignorePriceHint
                ? "- The user explicitly said to ignore price — set minPrice and maxPrice to null\n"
                : "";

        String promptTemplate = promptLoader.get(PromptKeys.INTENT_ENGLISH);
        String prompt = promptTemplate.formatted(priceInstruction, userQuestion);

        ChatResponse response = chatModel.chat(UserMessage.from(prompt));

        log.debug("[EnglishIntentAnalyzer] Raw intent JSON:\n{}", response.aiMessage().text());

        try {
            // clean markdown backticks if LLM adds them
            String cleaned = response.aiMessage().text()
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            SearchIntent searchIntent = mapper.readValue(cleaned, SearchIntent.class);

            return TokenUtil.buildAiResult(response, searchIntent);

        } catch (JsonProcessingException e) {
            log.warn("[EnglishIntentAnalyzer] Failed to parse intent, falling back to pure semantic search: {}", e.getMessage());
            // fallback — treat as pure semantic search

            SearchIntent searchIntent =  SearchIntent.builder()
                    .searchType("semantic")
                    .semanticQuery(userQuestion)
                    .build();

            return TokenUtil.buildAiResult(response, searchIntent);
        }
    }
}
