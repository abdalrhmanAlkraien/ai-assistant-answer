package com.project.ai.processing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.dto.SearchIntent;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:08 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class IntentAnalyzer {

    private final ChatModel chatModel;
    private final ObjectMapper mapper;

    public String enrichWithMemory(String question, String memoryContext) {

        log.info("Start enrich memory");
        if (memoryContext == null || memoryContext.isBlank()) {
            log.info("Memory context is blank");
            return question;
        }

        String prompt = """
                Given this conversation history:
                %s
                
                The user now asks: "%s"
                
                FIRST: Determine if the user is asking for a NEW product search OR an operation on previous results.
                
                Operation keywords: sort, order, arrange, group, filter, rank, compare, organize, ascending, descending, cheapest first, most expensive first, alphabetically.
                
                IF the user is asking for an operation (sort/order/filter/rank):
                - Return the question as-is, preserving the operation intent exactly
                - Do NOT replace with a product search query
                - Example: "sort products ascending" → "sort products ascending"
                
                IF the user is asking for a new product search with vague references:
                - Rewrite with context from history
                - If the user introduces a NEW topic/category, keep it — do NOT replace with history topic
                - If the user adds a price filter like "under X", carry the current category forward
                - If the user says "without care about price"/"ignore price"/"any price", drop price from the rewrite
                - PRESERVE all numeric values exactly as stated
                - NEVER replace numbers with vague words like "cheap", "affordable", "expensive"
                
                Examples:
                History: "laptops under $1000 → HP Pavilion 15"
                Question: "sort products ascending"
                Correct: "sort products ascending"  ✅
                Wrong:   "laptops under $1000 sort ascending"  ❌
                
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
                
                Return ONLY the rewritten question, nothing else.
                """.formatted(memoryContext, question);

        try {
            String enriched = chatModel.chat(prompt);
            log.info("Enriched from '{}' to '{}'", question, enriched);
            return enriched.trim();
        } catch (Exception e) {
            log.warn("Failed to enrich question: {}", e.getMessage());
            return question;
        }
    }


    public SearchIntent extractIntent(String userQuestion) {

        log.info("build search intent");

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
                
                Search types:
                - "price"      → user asks about price range only
                - "category"   → user asks about a product category
                - "brand"      → user asks to SEE products from a brand
                - "hybrid"     → combination of two or more filters (brand+price, category+price, etc.)
                - "semantic"   → user asks for recommendations or best product for a use case
                - "knowledge"  → user asks a general question or how something works (no product search needed)
                - "comparison" → user asks to compare, rank, find cheaper/better/best between specific products already mentioned in history
                - "suggest"    → user asks for alternatives or similar products
                - "sort"       → user wants to sort, order, rank, or arrange already returned products by price or name
                
                For "sort" type: set sortDirection to "asc" for ascending/cheapest first, "desc" for descending/most expensive first.
                For all other types: set sortDirection to null.
                
                Examples:
                "Compare iPhone vs Samsung"            → knowledge
                "What is the difference between OLED and QLED?" → knowledge
                "Show me Samsung products"             → brand
                "Best gaming laptop"                   → semantic
                "which one is cheaper?"                → comparison
                "which is better value?"               → comparison
                "what is the price difference?"        → comparison
                "products between 100 and 500"         → price
                "show me laptops under $500"           → hybrid  (category + price)
                "show me Samsung laptops under $500"   → hybrid  (brand + category + price)
                "sort products ascending"              → sort, sortDirection: "asc"
                "order by price descending"            → sort, sortDirection: "desc"
                "cheapest first"                       → sort, sortDirection: "asc"
                "most expensive first"                 → sort, sortDirection: "desc"
                "show me alternatives"                 → suggest
                "give me something similar"            → suggest
                
                User question: %s
                """.formatted(userQuestion);
        String intentJson = chatModel.chat(intentPrompt);
        log.info("Raw intent JSON from LLM: {}", intentJson);

        try {
            // clean markdown backticks if LLM adds them
            String cleaned = intentJson
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return mapper.readValue(cleaned, SearchIntent.class);

        } catch (JsonProcessingException e) {
            log.warn("Failed to parse intent, falling back to pure semantic search: {}", e.getMessage());
            // fallback — treat as pure semantic search
            return SearchIntent.builder()
                    .searchType("semantic")
                    .semanticQuery(userQuestion)
                    .build();
        }
    }
}
