package com.project.ai.util;

import com.project.ai.dto.AiResult;
import com.project.ai.dto.SearchIntent;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import lombok.experimental.UtilityClass;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 3:07 AM
 */
@UtilityClass
public class TokenUtil {

    public AiResult<SearchIntent> buildAiResult(ChatResponse response, SearchIntent searchIntent) {
        TokenUsage usage = response.tokenUsage();
        int inputTokens  = usage != null && usage.inputTokenCount()  != null ? usage.inputTokenCount()  : 0;
        int outputTokens = usage != null && usage.outputTokenCount() != null ? usage.outputTokenCount() : 0;
        return new AiResult<>(searchIntent, inputTokens, outputTokens);
    }

    public AiResult<String> buildCustomResult(int inputTokens, int outputTokens, String answer) {
        return new AiResult<>(answer, inputTokens, outputTokens);
    }
}
