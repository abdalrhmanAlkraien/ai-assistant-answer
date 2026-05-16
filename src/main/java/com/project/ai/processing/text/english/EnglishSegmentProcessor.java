package com.project.ai.processing.text.english;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:26 PM
 */
@Service
@Log4j2
public class EnglishSegmentProcessor implements ChatProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            "semantic", "price", "category", "brand", "hybrid", "comparison");

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final FilterProcessor filterProcessor;
    private final EnglishSuggestionProcessor englishSuggestionProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;

    public EnglishSegmentProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier("englishChatModel") final ChatModel chatModel,
            final FilterProcessor filterProcessor,
            final EnglishSuggestionProcessor englishSuggestionProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final SearchService searchService
    ) {

        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.filterProcessor = filterProcessor;
        this.englishSuggestionProcessor = englishSuggestionProcessor;
        this.matchedIdsResolver = matchedIdsResolver;
        this.searchService = searchService;
    }

    @Override
    public boolean supports(String searchType) {
        return SUPPORTED.contains(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[EnglishSegmentProcessor] START — type={}", request.getSearchIntent().getSearchType());
        SearchIntent intent = request.getSearchIntent();

        log.info("[EnglishSegmentProcessor] Building search request — semantic='{}'",
                intent.getSemanticQuery());

        EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(intent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        log.info("[EnglishSegmentProcessor] Vector search returned {} matches", matches.size());


        FilteredContext filteredContext = filterProcessor.filter(matches, intent);
        log.info("[EnglishSegmentProcessor] After filtering: {} products", filteredContext.getFilteredMatches().size());
        log.debug("[EnglishSegmentProcessor] Filtered products:\n{}", filteredContext.getContext());

        if (filteredContext.getFilteredMatches().isEmpty()) {
            log.info("[EnglishSegmentProcessor] No products after filtering — delegating to SuggestionProcessor");
            request.setVectorMatches(matches);
            return englishSuggestionProcessor.process(request);
        }

        String prompt = buildPrompt(intent, filteredContext,
                request.getRawQuestion(), request.getMemoryContext());

        log.debug("[EnglishSegmentProcessor] Prompt sent to LLM:\n{}", prompt);

        String answer = chatModel.chat(prompt);
        log.debug("[EnglishSegmentProcessor] LLM answer:\n{}", answer);

        matchedIdsResolver.resolve(answer, filteredContext, intent);

        // 5. Extract matched IDs
        List<String> matchedIds = matchedIdsResolver.resolve(answer, filteredContext, intent);
        log.info("[EnglishSegmentProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
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
                    - %s
                    - Prioritize the product that BEST matches the use case, not the cheapest one
                    - Be concise and explain why each product fits
                    - At the end list: "Product IDs: ..."
                    
                    Products:
                    %s
                    
                    %sAnswer:
                    """.formatted(
                    intent.getSemanticQuery(),
                    intent.getMaxPrice() != null ? "- Max price: $" + intent.getMaxPrice() + "\n" : "",
                    intent.getCategory() != null ? "- Category: " + intent.getCategory() + "\n" : "",
                    intent.getMaxPrice() != null
                            ? "Respect the price constraint — do NOT recommend products above $" + intent.getMaxPrice()
                            : "There is NO price constraint — recommend the BEST product for the use case regardless of price",
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
}
