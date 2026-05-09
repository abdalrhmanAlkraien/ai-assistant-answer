package com.project.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.ChatResponse;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
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


    public ChatResponse chat(final ChatRequest chatRequest) throws JsonProcessingException {

        SearchIntent searchIntent = extractIntent(chatRequest.getQuestion());

        if ("knowledge".equals(searchIntent.getSearchType())) {
            String answer = chatModel.chat("""
                You are a helpful assistant.
                Answer this question based on your knowledge.
                Be concise and helpful.
                
                Question: %s
                """.formatted(chatRequest.getQuestion()));

            return ChatResponse.builder()
                    .answer(answer)
                    .matchProducts(List.of())
                    .responseTime(LocalDateTime.now())
                    .build();
        }

        EmbeddingSearchRequest searchRequest = buildSearchRequest(searchIntent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(searchRequest)
                .matches();

        log.info("Found {} matching products for question: {}", matches.size(), chatRequest.getQuestion());

        if (matches.isEmpty()) {
            return ChatResponse.builder()
                    .answer("Sorry, I couldn't find any products matching your request.")
                    .matchProducts(List.of())
                    .responseTime(LocalDateTime.now())
                    .build();
        }

        FilteredContext filteredContext = buildContext(matches, searchIntent);
        log.info("After filtering: {} products", filteredContext.getFilteredMatches().size());

        String answerPrompt = buildPrompt(searchIntent, filteredContext, chatRequest.getQuestion());

        log.info("Search intent: type={}, category={}, brand={}, semanticQuery={}",
                searchIntent.getSearchType(),
                searchIntent.getCategory(),
                searchIntent.getBrand(),
                searchIntent.getSemanticQuery()
        );

        log.info("prompt is {} :", answerPrompt);

        if (filteredContext.getFilteredMatches().isEmpty()) {
            return ChatResponse.builder()
                    .answer("Sorry, I couldn't find any products matching your request.")
                    .matchProducts(List.of())
                    .responseTime(LocalDateTime.now())
                    .build();
        }
        String answer = chatModel.chat(answerPrompt);

        List<String> matchedIds = filteredContext.getFilteredMatches().stream()
                .map(match -> match.embedded().metadata().getString("id"))
                .collect(Collectors.toList());

        // Step 8: For semantic search extract IDs from answer
        if ("semantic".equals(searchIntent.getSearchType())) {
            List<String> parsedIds = Arrays.stream(answer.split(",|\\n|\\s"))
                    .map(s -> s.replaceAll("[^P0-9]", "").trim())  // remove everything except P and digits
                    .filter(s -> s.matches("P\\d{3}"))
                    .distinct()
                    .collect(Collectors.toList());

            if (!parsedIds.isEmpty()) {
                matchedIds = parsedIds;
            }
        }

        return ChatResponse.builder()
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
        
        Examples:
        "Compare iPhone vs Samsung"  → knowledge
        "What is the difference between OLED and QLED?" → knowledge
        "Show me Samsung products"   → brand
        "Best gaming laptop"         → semantic
        
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

        Embedding queryEmbedding = embeddingModel
                .embed(intent.getSemanticQuery())
                .content();

        // ── Broad search → get ALL, LLM filters ──────────────────
        if (isBroadSearch(intent)) {
            return EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(50)
                    .minScore(0.0)
                    .build();
        }

        // ── Semantic search → topK is enough ─────────────────────
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(properties.getVectorStore().getChroma().getTopKMax())
                .minScore(properties.getVectorStore().getChroma().getDefaultMinScoreThreshold())
                .build();
    }

    private boolean isBroadSearch(SearchIntent intent) {
        return switch (intent.getSearchType()) {
            case "price", "category", "hybrid" -> true;  // need all data
            case "semantic", "brand", "knowledge" -> false;
            default -> false;
        };
    }

    private String buildPrompt(SearchIntent intent, FilteredContext filteredContext, String userQuestion) {
        String context = filteredContext.getContext();
        int count = filteredContext.getFilteredMatches().size();

        return switch (intent.getSearchType()) {
            case "price" -> """
                    You are a product listing assistant.
                    Java has already filtered these %d products for you.
                    Your ONLY job is to list ALL %d products. Do not skip any.
                    
                    Products:
                    %s
                    
                    List all %d products:
                    Format: "Product Name - $Price - Category"
                    """.formatted(count, count, context, count);


            case "category" -> """
                    You are a product listing assistant.
                    The user is looking for: "%s" products.
                    
                    Review the products below and include ONLY products that are related to "%s".
                    Rules:
                    - Include products whose category exactly matches or is a subcategory of "%s"
                    - Include products whose description or tags are clearly related to "%s"
                    - Exclude products that are clearly unrelated to "%s"
                    
                    Products:
                    %s
                    
                    List the relevant products:
                    Format: "Product Name - $Price - Category"
                    """.formatted(
                    intent.getCategory(),
                    intent.getCategory(),
                    intent.getCategory(),
                    intent.getCategory(),
                    intent.getCategory(),
                    context);

            case "brand" -> """
                    You are a product filter. Your ONLY job is to filter products by brand.
                    
                    Brand requested: %s
                    
                    Go through each product and check: does it belong to brand "%s"?
                    If YES → include it.
                    If NO → exclude it. No exceptions.
                    
                    Products:
                    %s
                    
                    List ONLY products from brand "%s".
                    Format: "Product Name - $Price - Category"
                    """.formatted(
                    intent.getBrand(), intent.getBrand(),
                    context,
                    intent.getBrand());

            case "hybrid" -> """
                    You are a product filter. Filter products by ALL of these criteria:
                    %s%s%s
                    
                    Only include products matching ALL criteria. Exclude everything else.
                    
                    Products:
                    %s
                    
                    Format: "Product Name - $Price - Category"
                    """.formatted(
                    intent.getMinPrice() != null ? "- Price >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- Price <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- Brand: " + intent.getBrand() + "\n" : "",
                    context);

            default -> """
        You are a helpful e-commerce assistant.
        Answer the user's question based ONLY on the products listed below.
        Each product starts with its ID in brackets like [P007].
        Be concise and helpful.
        At the end, list the IDs of products you mentioned as: "Product IDs: [list the actual IDs]"
        
        Products:
        %s
        
        User Question: %s
        
        Answer:
        """.formatted(context, userQuestion);
        };
    }

    private FilteredContext buildContext(List<EmbeddingMatch<TextSegment>> matches, SearchIntent intent) {

        List<EmbeddingMatch<TextSegment>> filtered = switch (intent.getSearchType()) {
            case "price" -> matches.stream()
                    .filter(match -> {
                        Double price = extractPrice(match.embedded().text());
                        if (price == null) return false;
                        boolean aboveMin = intent.getMinPrice() == null || price >= intent.getMinPrice();
                        boolean belowMax = intent.getMaxPrice() == null || price <= intent.getMaxPrice();
                        return aboveMin && belowMax;
                    })
                    .toList();

            case "category" -> matches.stream()
                    .filter(match -> intent.getCategory() != null &&
                            match.embedded().text().toLowerCase()
                                    .contains(intent.getCategory().toLowerCase()))
                    .toList();

            case "brand" -> matches.stream()
                    .filter(match -> {
                        if (intent.getBrand() == null) return false;
                        String text = match.embedded().text().toLowerCase();
                        // split by comma and check if any brand matches
                        return Arrays.stream(intent.getBrand().split(","))
                                .map(String::trim)
                                .anyMatch(brand -> text.contains(brand.toLowerCase()));
                    })
                    .toList();

            default -> matches;
        };

        String context = filtered.stream()
                .map(match -> "[" + match.embedded().metadata().getString("id") + "] "
                        + match.embedded().text())
                .collect(Collectors.joining("\n"));
//
//        String context = filtered.stream()
//                .map(match -> "- " + match.embedded().text())
//                .collect(Collectors.joining("\n"));

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
