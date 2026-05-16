package com.project.ai.processing;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.service.SearchService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 14/05/2026
 * @Time: 11:23 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SortProcessor implements ChatProcessor{

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final FilterProcessor filterProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;

    @Override
    public boolean supports(String searchType) {
        return "sort".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        SearchIntent intent = request.getSearchIntent();
        log.info("[SortProcessor] START — direction={}", intent.getSortDirection());

        // re-search using enriched question to get relevant products
        String query = request.getEnrichedQuestion() != null
                ? request.getEnrichedQuestion()
                : request.getRawQuestion();

        EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(intent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                .search(searchRequest)
                .matches();

        log.info("[SortProcessor] Vector search returned {} matches", matches.size());

        FilteredContext filteredContext = filterProcessor.filter(matches, intent);

        log.info("[SortProcessor] After filter: {} products",
                filteredContext.getFilteredMatches().size());


        List<EmbeddingMatch<TextSegment>> toSort = filteredContext.getFilteredMatches().isEmpty()
                ? matches
                : filteredContext.getFilteredMatches();

        boolean ascending = !"desc".equals(intent.getSortDirection());

        List<EmbeddingMatch<TextSegment>> sorted = toSort.stream()
                .sorted((a, b) -> {
                    Double priceA = extractPrice(a.embedded().text());
                    Double priceB = extractPrice(b.embedded().text());
                    if (priceA == null) return 1;
                    if (priceB == null) return -1;
                    return ascending
                            ? Double.compare(priceA, priceB)
                            : Double.compare(priceB, priceA);
                })
                .toList();

        String answer = sorted.stream()
                .map(m -> {
                    String title    = extractField(m.embedded().text(), "Title");
                    String price    = extractField(m.embedded().text(), "Price");
                    String category = extractField(m.embedded().text(), "Category");
                    return title + " - " + price + " - " + category;
                })
                .collect(Collectors.joining("\n"));

        FilteredContext sortedContext = FilteredContext.builder()
                .filteredMatches(sorted)
                .context(answer)
                .build();

        List<String> matchedIds = matchedIdsResolver.resolve("", sortedContext, intent);

        log.info("[SortProcessor] END — sorted={}, direction={}",
                sorted.size(), ascending ? "asc" : "desc");

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type("sort")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }

    private Double extractPrice(String content) {
        try {
            Pattern pattern = Pattern.compile("Price:\\s*(\\d+(?:\\.\\d+)?)\\s*USD");
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) return Double.parseDouble(matcher.group(1));
        } catch (Exception e) {
            log.warn("[SortProcessor] Failed to extract price from: {}", content);
        }
        return null;
    }

    private String extractField(String text, String field) {
        Pattern pattern = Pattern.compile(field + ":\\s*([^\\n]+?)(?=\\s+\\w+:|$)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
