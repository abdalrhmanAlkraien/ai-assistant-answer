package com.project.ai.processing;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.service.SuggestionService;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:24 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SuggestionProcessor implements ChatProcessor {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final SuggestionService suggestionService;
    private final FilterProcessor filterProcessor;

    @Override
    public boolean supports(String searchType) {
        return "suggest".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("Start Suggest process");
        SearchIntent originalIntent = request.getSearchIntent();
        SearchIntent suggestIntent = suggestionService.buildSuggestIntent(originalIntent);

        String query = suggestIntent.getSemanticQuery();
        Embedding embedding = embeddingModel.embed(query).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(50)
                .minScore(0.0)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        FilteredContext suggestContext = filterProcessor.filter(matches, suggestIntent);

        log.info("SuggestionProcessor: {} candidates after filter", suggestContext.getFilteredMatches().size());

        if (suggestContext.getFilteredMatches().isEmpty()) {
            return ProcessingResult.builder()
                    .enrichedQuestion(originalIntent.getSemanticQuery())
                    .type("suggest")
                    .answer("Sorry, no products found matching your criteria.")
                    .matchedIds(List.of())
                    .build();
        }

        String enrichedQuestion = request.getEnrichedQuestion() != null
                ? request.getEnrichedQuestion()
                : request.getRawQuestion();

        String answer = suggestionService.suggestionProduct(enrichedQuestion, suggestContext);

        List<String> matchedIds = suggestContext.getFilteredMatches().stream()
                .map(m -> m.embedded().metadata().getString("id"))
                .collect(Collectors.toList());

        return ProcessingResult.builder()
                .enrichedQuestion(originalIntent.getSemanticQuery())
                .type("suggest")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }
}
