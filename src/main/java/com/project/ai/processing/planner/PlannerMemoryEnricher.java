package com.project.ai.processing.planner;

import com.project.ai.agents.Language;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.TokenTracker;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 24/05/2026
 * @Time: 12:28 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class PlannerMemoryEnricher {

    @Qualifier("fastChatModel")
    private final ChatModel fastChatModel;
    private final LangChain4jProperties properties;

    private static final String ARABIC_PROMPT = """
        بناءً على سجل المحادثة التالي:
        %s
        
        يسأل المستخدم الآن: "%s"
        
        القواعد:
        - إذا كان السؤال واضحاً ومستقلاً (يذكر منتجات أو فئات محددة)، أعده كما هو بالإنجليزية فقط
        - أعد الصياغة فقط إذا كان السؤال يحتوي على ضمائر أو إشارات للسياق السابق
          (هذا، ذلك، أيهما، هي، هو، الأرخص، رتبها، قارنها، الأفضل منها)
        - لا تستبدل سؤالاً جديداً واضحاً بسياق قديم من السجل
        
        أمثلة:
        السجل: "عرض لابتوبات"  |  السؤال: "أريد هواتف ذكية"      →  "show me smartphones"         (موضوع جديد، أعده كما هو)
        السجل: "عرض لابتوبات"  |  السؤال: "رتبها تصاعدياً"        →  "sort laptops ascending"      (حل الإشارة)
        السجل: "عرض لابتوبات"  |  السؤال: "أيهما الأرخص؟"         →  "which laptop is cheapest?"   (حل الإشارة)
        السجل: "عرض هواتف"     |  السؤال: "أريد هواتف وسماعات"    →  "show me smartphones and headphones" (موضوع جديد)
        
        أعد فقط السؤال المُعاد صياغته بالإنجليزية، لا شيء آخر.
        """;

    private static final String ENGLISH_PROMPT = """
        Based on the following conversation history:
        %s
        
        The user is now asking: "%s"
        
        Rules:
        - If the question is CLEAR and STANDALONE (mentions specific products/categories), return it AS-IS
        - Only rewrite if the question has PRONOUNS or REFERENCES to previous context
          (it, they, that, this, which one, the cheapest, sort them, compare them)
        - NEVER replace a clear new question with old context
        
        Examples:
        History: "showed laptops"  |  Question: "show me smartphones"  →  "show me smartphones"  (new topic, return as-is)
        History: "showed laptops"  |  Question: "sort them"            →  "sort laptops by price" (resolve reference)
        History: "showed laptops"  |  Question: "which is cheapest?"   →  "which laptop is cheapest?" (resolve reference)
        
        Return only the rewritten question, nothing else.
        """;

    public String enrich(MultimodalRequest request, String memoryContext) {

        String question = request.getTextQuestion();
        Language language = request.getDetectedLanguage();
        TokenTracker tracker = request.getTokenTracker();

        log.info("[PlannerMemoryEnricher] START — language={} question='{}'", language, question);

        if (memoryContext == null || memoryContext.isBlank()) {
            log.info("[PlannerMemoryEnricher] No memory context — returning original question");
            return question;
        }

        String prompt = language == Language.ARABIC
                ? ARABIC_PROMPT.formatted(memoryContext, question)
                : ENGLISH_PROMPT.formatted(memoryContext, question);

        try {
            long start = System.currentTimeMillis();
            ChatResponse response = fastChatModel.chat(UserMessage.from(prompt));
            long duration = System.currentTimeMillis() - start;

            String enriched = response.aiMessage().text().trim();

            tracker.record(
                    "ambiguity-resolver",
                    properties.getChatModel().getModels().get("fast").getModelName(),
                    response.tokenUsage().inputTokenCount(),
                    response.tokenUsage().outputTokenCount(),
                    duration
            );

            log.info("[PlannerMemoryEnricher] enriched: '{}' → '{}'", question, enriched);

            return response.aiMessage().text().trim();

        } catch (Exception e) {
            log.warn("[PlannerMemoryEnricher] Failed — returning original question: {}", e.getMessage());
            return question;
        }
    }
}
