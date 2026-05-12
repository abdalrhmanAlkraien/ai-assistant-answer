package com.project.ai.processing;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:26 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SegmentProcessor implements ChatProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            "semantic", "price", "category", "brand", "hybrid", "comparison", "sort");

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final ChatModel chatModel;
    private final LangChain4jProperties properties;
    private final FilterProcessor filterProcessor;
    private final SuggestionProcessor suggestionProcessor;

    @Override
    public boolean supports(String searchType) {
        return SUPPORTED.contains(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("Start Segment Process");
        SearchIntent intent = request.getSearchIntent();

        EmbeddingSearchRequest searchRequest = buildSearchRequest(intent);
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

        log.info("SegmentProcessor: found {} vector matches", matches.size());

//        if (matches.isEmpty()) {
//            return emptyResult(request);
//        }

        FilteredContext filteredContext = filterProcessor.filter(matches, intent);

        if (filteredContext.getFilteredMatches().isEmpty()) {
            log.info("No products after filtering — delegating to SuggestionProcessor");
            request.setVectorMatches(matches);
            return suggestionProcessor.process(request);
        }

        String prompt = buildPrompt(intent, filteredContext,
                request.getRawQuestion(), request.getMemoryContext());

        log.info("SegmentProcessor prompt:\n{}", prompt);
        String answer = chatModel.chat(prompt);

        // 5. Extract matched IDs
        List<String> matchedIds = extractMatchedIds(answer, filteredContext, intent);

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }


    private EmbeddingSearchRequest buildSearchRequest(SearchIntent intent) {

        String query = resolveQuery(intent);

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

    private String resolveQuery(SearchIntent intent) {
        if (intent.getSemanticQuery() != null && !intent.getSemanticQuery().isBlank())
            return intent.getSemanticQuery();
        if (intent.getCategory() != null) return intent.getCategory();
        if (intent.getBrand() != null) return intent.getBrand();
        return "product";
    }

    private boolean isBroadSearch(SearchIntent intent) {
        return switch (intent.getSearchType()) {
            case "price", "category", "hybrid", "suggest" -> true;  // need all data
            case "semantic", "brand", "knowledge", "comparison" -> false;
            default -> false;
        };
    }


    private List<String> extractMatchedIds(
            String answer,
            FilteredContext context,
            SearchIntent intent) {

        List<String> ids = context.getFilteredMatches().stream()
                .map(m -> m.embedded().metadata().getString("id"))
                .collect(Collectors.toList());

        if ("semantic".equals(intent.getSearchType()) || "comparison".equals(intent.getSearchType())) {
            List<String> parsed = Arrays.stream(answer.split(",|\\n|\\s"))
                    .map(s -> s.replaceAll("[^P0-9]", "").trim())
                    .filter(s -> s.matches("P\\d{3}"))
                    .distinct()
                    .collect(Collectors.toList());
            if (!parsed.isEmpty()) return parsed;
        }

        return ids;
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
                    List the relevant products, one per line using this format (do not print this line):
                    Product Name - $Price - Category
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
            case "semantic" -> """
                    You are a helpful e-commerce assistant.
                    The user is looking for: "%s"
                    Answer based ONLY on the products listed below.
                    %s%s
                    Rules:
                    - Only recommend products that are relevant to the user's request
                    - Respect any price constraints — do NOT recommend products outside the price range
                    - Be concise and explain why each product fits
                    - At the end list: "Product IDs: ..."
                    
                    Products:
                    %s
                    
                    %sAnswer:
                    """.formatted(
                    intent.getSemanticQuery(),
                    intent.getMaxPrice() != null ? "- Max price: $" + intent.getMaxPrice() + "\n" : "",
                    intent.getCategory() != null ? "- Category: " + intent.getCategory() + "\n" : "",
                    context,
                    memorySection);

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

    private ProcessingResult emptyResult(ProcessingRequest request) {
        return ProcessingResult.builder()
                .enrichedQuestion(request.getSearchIntent().getSemanticQuery())
                .type(request.getSearchIntent().getSearchType())
                .answer("Sorry, I couldn't find any products matching your request.")
                .matchedIds(List.of())
                .build();
    }
}
