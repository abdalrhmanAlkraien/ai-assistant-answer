package com.project.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.ChatRequest;
import com.project.ai.dto.ChatResponse;
import com.project.ai.dto.SearchIntent;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

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

//        Embedding questionEmbedding  = embeddingModel.embed(chatRequest.getQuestion()).content();

        SearchIntent searchIntent = extractIntent(chatRequest.getQuestion());

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

        // Step 4: Build prompt
        // Step 4: Build context from matches
        String context = matches.stream()
                .map(match -> "- " + match.embedded().text())
                .collect(Collectors.joining("\n"));

        String answerPrompt = buildPrompt(searchIntent, context, chatRequest.getQuestion());

        String answer = chatModel.chat(answerPrompt);

        List<String> matchedIds = matches.stream()
                .map(match -> match.embedded().metadata().getString("id"))
                .collect(Collectors.toList());

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
        Return ONLY this JSON structure, nothing else:
        {
          "searchType": "semantic | price | category | brand | hybrid",
          "minPrice": null or number,
          "maxPrice": null or number,
          "category": null or string,
          "brand": null or string,
          "semanticQuery": "the cleaned search query"
        }
        
        Examples:
        "products between 100 and 500" → {"searchType":"price","minPrice":100,"maxPrice":500,"category":null,"brand":null,"semanticQuery":"products"}
        "show me Samsung phones" → {"searchType":"brand","minPrice":null,"maxPrice":null,"category":"Smartphones","brand":"samsung","semanticQuery":"Samsung phones"}
        "best gaming laptop" → {"searchType":"semantic","minPrice":null,"maxPrice":null,"category":null,"brand":null,"semanticQuery":"best gaming laptop"}
        "Nike shoes under 200" → {"searchType":"hybrid","minPrice":null,"maxPrice":200,"category":"Shoes","brand":"nike","semanticQuery":"Nike shoes"}
        
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
            case "semantic", "brand"           -> false; // topK is enough
            default                            -> false;
        };
    }

    private String buildPrompt(SearchIntent intent, String context, String userQuestion) {
        return switch (intent.getSearchType()) {
            case "price" -> """
                You are a product filter. Your ONLY job is to filter products by price.
                
                Price range requested: $%s to $%s USD
                
                Go through each product and check: is the price >= %s AND <= %s ?
                If YES → include it.
                If NO → exclude it. No exceptions.
                
                Products:
                %s
                
                List ONLY products where price is between $%s and $%s. Nothing else.
                Format: "Product Name - $Price - Category"
                """.formatted(
                    intent.getMinPrice(), intent.getMaxPrice(),
                    intent.getMinPrice(), intent.getMaxPrice(),
                    context,
                    intent.getMinPrice(), intent.getMaxPrice());

            case "category" -> """
                You are a product filter. Your ONLY job is to filter products by category.
                
                Category requested: %s
                
                Go through each product and check: does the category match "%s"?
                If YES → include it.
                If NO → exclude it. No exceptions.
                
                Products:
                %s
                
                List ONLY products in the "%s" category.
                Format: "Product Name - $Price - Category"
                """.formatted(
                    intent.getCategory(), intent.getCategory(),
                    context,
                    intent.getCategory());

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

            default -> // semantic, trending, best, etc.
                    """
                    You are a helpful e-commerce assistant.
                    Answer the user's question based ONLY on the products listed below.
                    Be concise and helpful.
                    
                    Products:
                    %s
                    
                    User Question: %s
                    
                    Answer:
                    """.formatted(context, userQuestion);
        };
    }


}
