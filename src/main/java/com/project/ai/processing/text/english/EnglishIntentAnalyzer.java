package com.project.ai.processing.text.english;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import dev.langchain4j.model.chat.ChatModel;
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
public class EnglishIntentAnalyzer implements IntentAnalyzer {

    private final ChatModel chatModel;
    private final ObjectMapper mapper;

    public EnglishIntentAnalyzer(
            @Qualifier("englishChatModel") final ChatModel chatModel,
            final ObjectMapper mapper
    ) {
        this.chatModel = chatModel;
        this.mapper = mapper;
    }

    @Override
    public String enrichWithMemory(String question, String memoryContext) {

        log.info("[EnglishIntentAnalyzer] enrichWithMemory — question='{}'", question);
        log.debug("[EnglishIntentAnalyzer] Memory context for enrichment:\n{}", memoryContext);

        if (memoryContext == null || memoryContext.isBlank()) {
            log.info("[EnglishIntentAnalyzer] - Memory context is blank");
            return question;
        }

        String prompt = """
                Given this conversation history:
                %s
                
                The user now asks: "%s"
                
                FIRST: Determine the type of request:
                1. OPERATION — sort, order, arrange, ascending, descending, cheapest first, most expensive first
                2. RECOMMENDATION — best, recommend, good for, suitable for, which one should I buy, what is the best
                3. COMPARISON — which is cheaper, which is better, price difference, more expensive, compare two products
                4. LISTING — show me, list, what are, give me
                5. NEW TOPIC — user introduces a completely new category or product type
                6. FILTER REFINEMENT — user adds price/brand/category constraint to previous results
                
                RULES:
                
                IF OPERATION (sort/order/ascending/descending):
                - Look at the MOST RECENT product search in history
                - Carry forward ONLY the category/brand from that last search
                - Do NOT go back to earlier searches in the conversation
                - Example output: "sort laptops ascending", "sort Samsung smartphones descending"
                
                IF RECOMMENDATION (best/good for/which one should I buy/what is the best):
                - Use "best" or "recommend" in the rewrite — NEVER use "show me"
                - Carry category and price constraints from most recent search
                - If user says ignore price / without care about price / any price → drop price constraint
                - Example output: "best laptop for gaming under $1000", "best laptop for gaming"
                
                IF COMPARISON (which is cheaper/better/more expensive/price difference):
                - NEVER convert to a recommendation ("best X") — preserve the comparison intent
                - NEVER add price constraints from unrelated previous context
                - Reference ONLY the products from the most recent search
                - Use "which is cheaper", "compare", "what is the price difference" in the rewrite
                - Example output: "which is cheaper, iPhone 15 Pro or Samsung Galaxy S24?"
                
                IF LISTING (show me/list/give me):
                - Use "show me" in the rewrite
                - Carry category forward from history if question is vague
                - Example output: "show me laptops under $1000", "show me Samsung smartphones"
                
                IF NEW TOPIC:
                - Keep the new topic exactly — do NOT replace with history topic
                - Example output: "show me smartphones" (even if last search was laptops)
                
                IF FILTER REFINEMENT (under X / only brand / add constraint):
                - Carry the MOST RECENT category/brand forward
                - Add the new constraint
                - PRESERVE all numeric values exactly — NEVER replace with "cheap" or "affordable"
                - Example output: "show me laptops under $1000"
                
                Examples:
                History: "User searched laptops → HP Pavilion, MacBook, Dell XPS"
                Question: "sort products ascending"
                Correct: "sort laptops ascending"  ✅
                Wrong:   "sort products ascending"  ❌
                Wrong:   "sort iPhone and Samsung ascending"  ❌
                
                History: "User searched Samsung smartphones → Galaxy S24"
                Question: "sort descending"
                Correct: "sort Samsung smartphones descending"  ✅
                
                History: "User was looking at laptops under $1000"
                Question: "what is the best one for gaming?"
                Correct: "best laptop for gaming under $1000"  ✅
                Wrong:   "show me laptops under $1000 for gaming"  ❌
                
                History: "User was looking at laptops under $1000"
                Question: "what is the best one for gaming without care about the price?"
                Correct: "best laptop for gaming"  ✅
                Wrong:   "best laptop for gaming under $1000"  ❌
                
                History: "User was looking at laptops"
                Question: "what about under 1000?"
                Correct: "show me laptops under $1000"  ✅
                Wrong:   "products under $1000"  ❌
                
                History: "comparing iPhone vs Samsung smartphones"
                Question: "show me laptops"
                Correct: "show me laptops"  ✅
                Wrong:   "show me iPhone and Samsung laptops"  ❌
                
                History: "User searched laptops"
                Question: "which one should I buy for work?"
                Correct: "best laptop for work"  ✅
                Wrong:   "show me laptops for work"  ❌
                
                History: "User searched laptops under $1000"
                Question: "which one is good for students?"
                Correct: "best laptop for students under $1000"  ✅
                Wrong:   "show me laptops under $1000 for students"  ❌
                
                History: "User found iPhone 15 Pro ($999) and Samsung Galaxy S24 ($899)"
                Question: "which one is cheaper?"
                Correct: "which is cheaper, iPhone 15 Pro or Samsung Galaxy S24?"  ✅
                Wrong:   "best smartphone under $1000"                             ❌
                
                History: "User found iPhone 15 Pro ($999) and Samsung Galaxy S24 ($899)"
                Question: "what is the price difference?"
                Correct: "what is the price difference between iPhone 15 Pro and Samsung Galaxy S24?"  ✅
                Wrong:   "price difference under $1000"                                                ❌
                
                History: "User searched Samsung smartphones → Galaxy S24 ($899)"
                Question: "which one is cheaper?"
                Correct: "which Samsung smartphone is cheaper?"  ✅
                Wrong:   "best smartphone under $1000"           ❌
                
                Return ONLY the rewritten question, nothing else.
                """.formatted(memoryContext, question);

        try {
            String enriched = chatModel.chat(prompt);
            log.info("[EnglishIntentAnalyzer] Enrichment result='{}'", enriched);
            return enriched.trim();
        } catch (Exception e) {
            log.warn("Failed to enrich question: {}", e.getMessage());
            return question;
        }
    }

    @Override
    public SearchIntent extractIntent(String userQuestion) {


        log.debug("[EnglishIntentAnalyzer] Raw intent JSON:\n{}", userQuestion);

        boolean ignorePriceHint = userQuestion.toLowerCase().matches(
                ".*\\b(without.*price|ignore.*price|no.*budget|any.*price|" +
                        "regardless.*price|price.*matter|don.t care.*price|" +
                        "care about.*price|without care)\\b.*");

        String priceInstruction = ignorePriceHint
                ? "- The user explicitly said to ignore price — set minPrice and maxPrice to null\n"
                : "";

        String intentPrompt = """
                Analyze this user question and extract search filters as JSON only.
                Return ONLY this JSON structure, nothing else, no markdown, no backticks:
                {
                  "searchType": "semantic | price | category | brand | hybrid | knowledge | comparison | suggest | sort",
                  "minPrice": null or number,
                  "maxPrice": null or number,
                  "category": null or string,
                  "brand": null or string,
                  "semanticQuery": "the cleaned search query",
                  "sortDirection": null or "asc" or "desc"
                }
                
                %s
                Search types:
                - "price"      → user asks about price range only
                - "category"   → user asks about a product category
                - "brand"      → user asks to SEE products from a brand
                - "hybrid"     → combination of two or more filters (brand+price, category+price, etc.)
                - "semantic"   → user asks for recommendations or best product for a use case
                - "knowledge"  → user asks a general question or how something works (no product search needed)
                - "comparison" → user asks to compare, rank, find cheaper/better/best between specific products
                                 ONLY extract brand if explicitly mentioned in the question
                                 NEVER extract category from comparison questions — set category to null always
                                 sortDirection must always be null for comparison
                - "suggest"    → user asks for alternatives or similar products
                - "sort"       → user wants to sort already returned products by price or name
                                 extract category and brand from the sort query if present
                                 "sort laptops ascending" → category: "laptops", sortDirection: "asc"
                                 "sort Samsung phones descending" → brand: "Samsung", sortDirection: "desc"
                
                For "sort" type: set sortDirection to "asc" for ascending/cheapest first, "desc" for descending/most expensive first.
                For "comparison" type: set sortDirection to null always, set category to null always.
                For all other types: set sortDirection to null.
                
                Examples:
                "Compare iPhone vs Samsung"                    → knowledge
                "What is the difference between OLED and QLED?" → knowledge
                "Show me Samsung products"                     → brand
                "Best gaming laptop"                           → semantic
                "which one is cheaper?"                        → comparison, brand: null, category: null, sortDirection: null
                "which Samsung smartphone is cheaper?"         → comparison, brand: "Samsung", category: null, sortDirection: null
                "which is better value?"                       → comparison, brand: null, category: null, sortDirection: null
                "what is the price difference?"                → comparison, brand: null, category: null, sortDirection: null
                "which is better, iPhone or Samsung?"          → comparison, brand: null, category: null, sortDirection: null
                "products between 100 and 500"                 → price
                "show me laptops under $500"                   → hybrid
                "show me Samsung laptops under $500"           → hybrid
                "sort laptops ascending"                       → sort, category: "laptops", sortDirection: "asc"
                "sort Samsung phones descending"               → sort, brand: "Samsung", sortDirection: "desc"
                "order by price descending"                    → sort, sortDirection: "desc"
                "cheapest first"                               → sort, sortDirection: "asc"
                "most expensive first"                         → sort, sortDirection: "desc"
                "show me alternatives"                         → suggest
                "give me something similar"                    → suggest
                
                User question: %s
                """.formatted(priceInstruction, userQuestion);

        String intentJson = chatModel.chat(intentPrompt);
        log.debug("[EnglishIntentAnalyzer] Raw intent JSON:\n{}", intentJson);

        try {
            // clean markdown backticks if LLM adds them
            String cleaned = intentJson
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return mapper.readValue(cleaned, SearchIntent.class);

        } catch (JsonProcessingException e) {
            log.warn("[EnglishIntentAnalyzer] Failed to parse intent, falling back to pure semantic search: {}", e.getMessage());
            // fallback — treat as pure semantic search
            return SearchIntent.builder()
                    .searchType("semantic")
                    .semanticQuery(userQuestion)
                    .build();
        }
    }
}
