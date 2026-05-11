package com.project.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.ChatResponse;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import com.project.ai.model.MessageRole;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/05/2026
 * @Time: 10:39 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ChatService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;
    private final ObjectMapper mapper;
    private final MemoryService memoryService;
    private final SuggestionService suggestionService;

    public ChatResponse chat(final Long userId, final ChatRequest chatRequest) throws JsonProcessingException {


        String context = memoryService.memoryContext(userId, chatRequest.getQuestion());

        String questionWithHistory = enrichWithMemory(chatRequest.getQuestion(), context);

        log.info("here is the updated question after merge with history {}", questionWithHistory);
        SearchIntent searchIntent = extractIntent(questionWithHistory);

        if ("knowledge".equals(searchIntent.getSearchType())) {
            String answer = chatModel.chat("""
                    You are a helpful assistant.
                    Answer this question based on your knowledge.
                    Be concise and helpful.
                    
                    Question: %s
                    """.formatted(chatRequest.getQuestion()));

            saveInMemory(userId, searchIntent, answer, null);

            return ChatResponse.builder()
                    .question(searchIntent.getSemanticQuery())
                    .type(searchIntent.getSearchType())
                    .answer(answer)
                    .matchProducts(List.of())
                    .responseTime(LocalDateTime.now())
                    .build();
        }

        log.info("SemanticQuery {}", searchIntent.getSemanticQuery());
        EmbeddingSearchRequest searchRequest = buildSearchRequest(searchIntent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(searchRequest)
                .matches();

        log.info("Found {} matching products for question: {}", matches.size(), chatRequest.getQuestion());

        if (matches.isEmpty()) {
            return ChatResponse.builder()
                    .question(searchIntent.getSemanticQuery())
                    .type(searchIntent.getSearchType())
                    .answer("Sorry, I couldn't find any products matching your request.")
                    .matchProducts(List.of())
                    .responseTime(LocalDateTime.now())
                    .build();
        }

        FilteredContext filteredContext = buildContext(matches, searchIntent);

        String answerPrompt;

        List<String> matchedIds;
        String answer;
        FilteredContext activeContext;

        if(filteredContext.getFilteredMatches().isEmpty()) {

            log.info("no product found");
            // Case 1: matches also empty → no products at all
            if (matches.isEmpty()) {
                return ChatResponse.builder()
                        .question(chatRequest.getQuestion())
                        .type(searchIntent.getSearchType())
                        .answer("Sorry, I couldn't find any products matching your request.")
                        .matchProducts(List.of())
                        .responseTime(LocalDateTime.now())
                        .build();
            }

            SearchIntent suggestIntent = SearchIntent.builder()
                    .searchType("suggest")
                    .category(searchIntent.getCategory())
                    .minPrice(searchIntent.getMinPrice())
                    .maxPrice(searchIntent.getMaxPrice())
                    .semanticQuery(searchIntent.getCategory() != null
                            ? searchIntent.getCategory()
                            : searchIntent.getSemanticQuery())
                    .build();

            // Search again with suggest intent
            EmbeddingSearchRequest suggestRequest = buildSearchRequest(suggestIntent);

            matches = embeddingStore
                    .search(suggestRequest)
                    .matches();

            FilteredContext suggestContext = buildContext(matches, suggestIntent);
            log.info("Suggest filtered: {} products", suggestContext.getFilteredMatches().size());

            if (suggestContext.getFilteredMatches().isEmpty()) {
                return ChatResponse.builder()
                        .question(chatRequest.getQuestion())
                        .type("suggest")
                        .answer("Sorry, no products found matching your criteria.")
                        .matchProducts(List.of())
                        .responseTime(LocalDateTime.now())
                        .build();
            }
            activeContext = suggestContext;
            // Case 2: matches exist but filtered to empty → suggest alternatives
            answerPrompt = suggestionService.suggestionProduct(questionWithHistory, suggestContext);

        } else {

            log.info("After filtering: {} products", filteredContext.getFilteredMatches().size());

            answerPrompt = buildPrompt(searchIntent, filteredContext, chatRequest.getQuestion(), context);
            activeContext = filteredContext;
        }

        log.info("Search intent: type={}, category={}, brand={}, semanticQuery={}",
                searchIntent.getSearchType(),
                searchIntent.getCategory(),
                searchIntent.getBrand(),
                searchIntent.getSemanticQuery()
        );

        log.info("prompt is {} :", answerPrompt);
        answer = chatModel.chat(answerPrompt);

        matchedIds = activeContext.getFilteredMatches().stream()
                .map(match -> match.embedded().metadata().getString("id"))
                .collect(Collectors.toList());

        // Step 8: For semantic search extract IDs from answer
        if ("semantic".equals(searchIntent.getSearchType()) ||
                "comparison".equals(searchIntent.getSearchType())) {

            List<String> parsedIds = Arrays.stream(answer.split(",|\\n|\\s"))
                    .map(s -> s.replaceAll("[^P0-9]", "").trim())
                    .filter(s -> s.matches("P\\d{3}"))
                    .distinct()
                    .collect(Collectors.toList());

            if (!parsedIds.isEmpty()) {
                matchedIds = parsedIds;
            }
        }
        String[] matchedProducts = activeContext.getFilteredMatches().stream()
                .map(match -> match.embedded().metadata().getString("id"))
                .toArray(String[]::new);


        saveInMemory(userId, searchIntent, answer, matchedProducts);

        return ChatResponse.builder()
                .question(searchIntent.getSemanticQuery())
                .type(searchIntent.getSearchType())
                .answer(answer)
                .matchProducts(matchedIds)
                .responseTime(LocalDateTime.now())
                .build();
        /**
         * Type of search on the vector DB is
         * 1- Semantic Search
         * 2- Exact search
         * 3- Range filter
         */

        /**
         * Question what is the  TextSegment here   private final EmbeddingStore<TextSegment> embeddingStore;
         * what is the type of EmbeddingStore?
         */
        /**
         * 1-  we need embeded questino becouse the data store as a vectors
         * 2-
         */
    }

    private String enrichWithMemory(String question, String memoryContext) {
        if (memoryContext == null || memoryContext.isBlank()) {
            return question;
        }

        String enrichPrompt = """
        Given this conversation history:
        %s
        
        The user now asks: "%s"
        
        Rewrite the user's question as a standalone question that includes
        all necessary context from the conversation history.
        
        STRICT RULES:
        - Keep the SAME intent as the original question
        - ONLY add missing context (brands, categories, price ranges)
        - Do NOT change the question type
        - Preserve the SPECIFIC topic/category from the history context
        - Do NOT expand to unrelated categories or broader product ranges
        - PRESERVE all numeric values exactly as stated (prices, quantities)
        - NEVER replace numbers with vague words like "cheap", "affordable", "expensive"
        
        Examples:
        History: "User was comparing [BrandA] and [BrandB] [Category]"
        Question: "what about under 500?"
        Correct: "show me [BrandA] and [BrandB] [Category] under $500"  ✅
        Wrong:   "show me cheap [BrandA] and [BrandB] [Category]"  ❌
        
        History: "User was looking at [Category]"
        Question: "show me the cheap ones"
        Correct: "show me cheap [Category]"  ✅
        
        History: "User was looking at [Category]"
        Question: "what about under 300?"
        Correct: "show me [Category] under $300"  ✅
        Wrong:   "show me affordable [Category]"  ❌
        
        Return ONLY the rewritten question, nothing else.
        """.formatted(memoryContext, question);

        try {
            String enriched = chatModel.chat(enrichPrompt);
            log.info("Enriched from '{}' to '{}'", question, enriched);
            return enriched.trim();
        } catch (Exception e) {
            log.warn("Failed to enrich question: {}", e.getMessage());
            return question;
        }
    }

    private void saveInMemory(final Long userId, final SearchIntent searchIntent, final String answer, final String[] matchedProducts) {

        memoryService.saveMemory(
                userId,
                searchIntent,
                MessageRole.USER,
                searchIntent.getSemanticQuery(),
                matchedProducts);

        memoryService.saveMemory(
                userId,
                searchIntent,
                MessageRole.AI,
                answer,
                matchedProducts);
    }
    // ─── Extract Intent ───────────────────────────────────────

    private SearchIntent extractIntent(String userQuestion) {

        log.info("build search intent");

        String intentPrompt = """
                Analyze this user question and extract search filters as JSON only.
                Return ONLY this JSON structure, nothing else, no markdown, no backticks:
                {
                  "searchType": "semantic | price | category | brand | hybrid | knowledge",
                  "minPrice": null or number,
                  "maxPrice": null or number,
                  "category": null or string,
                  "brand": null or string,
                  "semanticQuery": "the cleaned search query"
                }
                
                Search types:
                - "price"     → user asks about price range
                - "category"  → user asks about a product category
                - "brand"     → user asks to SEE products from a brand
                - "hybrid"    → combination of filters
                - "semantic"  → user asks for recommendations
                - "knowledge" → user asks a general question, comparison, or how something works
                - "comparison"  → user asks to compare, rank, find cheaper/better/best\s
                                   between specific products already mentioned in history
                
                Examples:
                "Compare iPhone vs Samsung"  → knowledge
                "What is the difference between OLED and QLED?" → knowledge
                "Show me Samsung products"   → brand
                "Best gaming laptop"         → semantic
                "which one is cheaper?"          → comparison
                "which is better value?"         → comparison
                "what is the price difference?"  → comparison
                "products between 100 and 500"   → price
                "show me Samsung products"       → brand
                
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


    private EmbeddingSearchRequest buildSearchRequest(SearchIntent intent) {

        String query = (intent.getSemanticQuery() != null && !intent.getSemanticQuery().isBlank())
                ? intent.getSemanticQuery()
                : intent.getCategory() != null ? intent.getCategory()
                : intent.getBrand() != null ? intent.getBrand()
                : "product";

        Embedding queryEmbedding = embeddingModel
                .embed(query)
                .content();

        if (isBroadSearch(intent)) {
            return EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(50)
                    .minScore(0.0)
                    .build();
        }

        return EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(properties.getVectorStore().getChroma().getTopKMax())
                .minScore(properties.getVectorStore().getChroma().getDefaultMinScoreThreshold())
                .build();
    }

    private boolean isBroadSearch(SearchIntent intent) {
        return switch (intent.getSearchType()) {
            case "price", "category", "hybrid", "suggest" -> true;  // need all data
            case "semantic", "brand", "knowledge" , "comparison" -> false;
            default -> false;
        };
    }

    private String buildPrompt(
            final SearchIntent intent,
            final FilteredContext filteredContext,
            final String userQuestion,
            final String memoryContext) {

        String memorySection = memoryContext.isEmpty() ? "" : """
                User conversation history:
                %s
                
                """.formatted(memoryContext);

        String context = filteredContext.getContext();
        int count = filteredContext.getFilteredMatches().size();

        return switch (intent.getSearchType()) {
            case "price" -> """
                    You are a product listing assistant.
                    Java has already filtered these %d products for you.
                    Your ONLY job is to list ALL %d products. Do not skip any.
                    
                    Products:
                    %s
                    
                    %s
                    List all %d products:
                    Format: "Product Name - $Price - Category"
                    """.formatted(count, count, context, memorySection, count);


            case "category" -> """
                    You are a product listing assistant.
                    The user is looking for: "%s" products.
                    
                    Review the products below and include ONLY products related to "%s".
                    Rules:
                    - Include products whose category exactly matches or is a subcategory of "%s"
                    - Include products whose description or tags are clearly related to "%s"
                    - Exclude products that are clearly unrelated to "%s"
                    
                    Products:
                    %s
                    
                    %s
                    List the relevant products:
                    Format: "Product Name - $Price - Category"
                    """.formatted(
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(),
                    context,
                    memorySection);

            case "brand" -> """
                    You are a product listing assistant.
                    The user is looking for products from these brands: "%s".
                    
                    Review the products below and include ONLY products that belong to any of these brands.
                    If a product is clearly from a different brand, exclude it.
                    
                    Products:
                    %s
                    
                    %s
                    List the relevant products:
                    Format: "Product Name - $Price - Category"
                    """.formatted(intent.getBrand(), context, memorySection);

            case "hybrid" -> """
                    You are a product filter. Filter products by ALL of these criteria:
                    %s%s%s
                    
                    Only include products matching ALL criteria. Exclude everything else.
                    
                    Products:
                    %s
                    
                    %s
                    Format: "Product Name - $Price - Category"
                    """.formatted(
                    intent.getMinPrice() != null ? "- Price >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- Price <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- Brand: " + intent.getBrand() + "\n" : "",
                    context, memorySection);

            case "comparison" -> """
                    You are a helpful e-commerce assistant.
                    Compare the products below and answer the user's question directly.
                    Be concise — give a clear winner or direct answer.
                    At the end list: "Product IDs: ..."
                    
                    Products:
                    %s
                    
                    %s
                    User Question: %s
                    
                    Answer:
                    """.formatted(context, memorySection, userQuestion);

            default -> """
                    You are a helpful e-commerce assistant.
                    Answer the user's question based ONLY on the products listed below.
                    Each product starts with its ID in brackets.
                    Be concise and helpful.
                    At the end, list the IDs of products you mentioned as: "Product IDs: ..."
                    
                    Products:
                    %s
                    
                    %s
                    User Question: %s
                    
                    Answer:
                    """.formatted(context, memorySection, userQuestion);
        };
    }

    private FilteredContext buildContext(List<EmbeddingMatch<TextSegment>> matches, SearchIntent intent) {

        List<EmbeddingMatch<TextSegment>> filtered = switch (intent.getSearchType()) {
            case "price" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());
                        if (price == null) return false;

                        boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                        boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                        boolean priceMatch = aboveMin && belowMax;

                        // Also filter by category if present
                        boolean categoryMatch = intent.getCategory() == null ||
                                text.contains(intent.getCategory().toLowerCase());

                        return priceMatch && categoryMatch;
                    })
                    .toList();

            case "category" -> matches.stream()
                    .filter(match -> intent.getCategory() != null &&
                            match.embedded().text().toLowerCase()
                                    .contains(intent.getCategory().toLowerCase()))
                    .toList();

            case "comparison" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();

                        // Filter by brand if present
                        boolean brandMatch = true;
                        if (intent.getBrand() != null) {
                            brandMatch = Arrays.stream(intent.getBrand().split(","))
                                    .map(String::trim)
                                    .anyMatch(brand -> text.contains(brand.toLowerCase()));
                        }

                        // Filter by category if present
                        boolean categoryMatch = true;
                        if (intent.getCategory() != null) {
                            categoryMatch = text.contains(intent.getCategory().toLowerCase());
                        }

                        return brandMatch && categoryMatch;
                    })
                    .toList();

            case "brand" -> matches.stream()
                    .filter(match -> {
                        if (intent.getBrand() == null) return false;
                        String text = match.embedded().text().toLowerCase();

                        // Check brand matches
                        boolean brandMatch = Arrays.stream(intent.getBrand().split(","))
                                .map(String::trim)
                                .anyMatch(brand -> text.contains(brand.toLowerCase()));

                        // Check category if present in intent
                        if (brandMatch && intent.getCategory() != null) {
                            return text.contains(intent.getCategory().toLowerCase());
                        }

                        return brandMatch;
                    })
                    .toList();

            case "hybrid" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());

                        // Price filter
                        boolean priceMatch = true;
                        if (price != null) {
                            boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                            boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                            priceMatch = aboveMin && belowMax;
                        }

                        // Brand filter
                        boolean brandMatch = true;
                        if (intent.getBrand() != null) {
                            brandMatch = Arrays.stream(intent.getBrand().split(","))
                                    .map(String::trim)
                                    .anyMatch(brand -> text.contains(brand.toLowerCase()));
                        }

                        // Category filter
                        boolean categoryMatch = true;
                        if (intent.getCategory() != null) {
                            categoryMatch = text.contains(intent.getCategory().toLowerCase());
                        }

                        return priceMatch && brandMatch && categoryMatch;
                    })
                    .toList();

            case "suggest" -> matches.stream()
                    .filter(match -> {
                        String text = match.embedded().text().toLowerCase();
                        Double price = extractPrice(match.embedded().text());

                        // Price filter
                        boolean priceMatch = true;
                        if (price != null) {
                            boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                            boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                            priceMatch = aboveMin && belowMax;
                        }

                        // Category filter — no brand filter
                        boolean categoryMatch = intent.getCategory() == null ||
                                text.contains(intent.getCategory().toLowerCase());

                        return priceMatch && categoryMatch;
                    })
                    .toList();

            default -> matches;
        };

        String context = filtered.stream()
                .map(match -> "[" + match.embedded().metadata().getString("id") + "] "
                        + match.embedded().text())
                .collect(Collectors.joining("\n"));


        return FilteredContext.builder()
                .context(context)
                .filteredMatches(filtered)
                .build();
    }

    private Double extractPrice(String content) {
        try {
            Pattern pattern = Pattern.compile("Price:\\s*(\\d+(?:\\.\\d+)?)\\s*USD");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.warn("Failed to extract price: {}", content);
        }
        return null;
    }
}
