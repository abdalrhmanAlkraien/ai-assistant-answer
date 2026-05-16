package com.project.ai.processing.text.arabic;

import com.project.ai.agents.Language;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
import com.project.ai.service.SuggestionService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:28 PM
 */
@Service
@Log4j2
public class ArabicSuggestionProcessor implements ChatProcessor {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final SuggestionService suggestionService;
    private final FilterProcessor arabicFilterProcessor;
    private final MatchedIdsResolver matchedIdsResolver;
    private final SearchService searchService;
    private final ChatModel chatModel;

    public ArabicSuggestionProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            final SuggestionService suggestionService,
            final FilterProcessor arabicFilterProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final SearchService searchService,
            @Qualifier("arabicChatModel") ChatModel chatModel
    ) {

        this.embeddingStore = embeddingStore;
        this.suggestionService = suggestionService;
        this.matchedIdsResolver = matchedIdsResolver;
        this.arabicFilterProcessor = arabicFilterProcessor;
        this.searchService = searchService;
        this.chatModel = chatModel;
    }

    @Override
    public boolean supports(String searchType) {
        return "suggest".equals(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[ArabicSuggestionProcessor] START — originalType={}",
                request.getSearchIntent().getSearchType());

        SearchIntent originalIntent = request.getSearchIntent();

        // keep relaxing constraints until we find candidates
        SearchIntent relaxedIntent = originalIntent;

        FilteredContext suggestContext = FilteredContext.builder()
                .filteredMatches(List.of())
                .context("")
                .build();

        for (int step = 1; step <= 4; step++) {
            relaxedIntent = suggestionService.buildSuggestIntent(relaxedIntent);

            String query = relaxedIntent.getSemanticQuery() != null
                    ? relaxedIntent.getSemanticQuery()
                    : originalIntent.getSemanticQuery();

            log.info("[ArabicSuggestionProcessor] Relaxation step {} — brand={}, maxPrice={}, query='{}'",
                    step, relaxedIntent.getBrand(), relaxedIntent.getMaxPrice(), query);

            EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(relaxedIntent);

            List<EmbeddingMatch<TextSegment>> matches = embeddingStore
                    .search(searchRequest)
                    .matches();

            suggestContext = arabicFilterProcessor.filter(matches, relaxedIntent);

            log.info("[ArabicSuggestionProcessor] Step {} candidates: {}",
                    step, suggestContext.getFilteredMatches().size());

            if (!suggestContext.getFilteredMatches().isEmpty()) {
                log.info("[ArabicSuggestionProcessor] Found candidates at step {}", step);
                break;
            }
        }

        if (suggestContext.getFilteredMatches().isEmpty()) {
            log.info("[ArabicSuggestionProcessor] No candidates found after all relaxation steps");
            return ProcessingResult.builder()
                    .enrichedQuestion(originalIntent.getSemanticQuery())
                    .type("suggest")
                    .answer("عذراً، لم نجد منتجات تطابق معاييرك.")
                    .matchedIds(List.of())
                    .build();
        }


        // cap to top 5 by score to avoid passing irrelevant products to LLM
        List<EmbeddingMatch<TextSegment>> topMatches = suggestContext.getFilteredMatches().stream()
                .sorted((a, b) -> Double.compare(b.score(), a.score()))
                .limit(5)
                .toList();

        FilteredContext cappedContext = FilteredContext.builder()
                .filteredMatches(topMatches)
                .context(topMatches.stream()
                        .map(m -> "[" + m.embedded().metadata().getString("id") + "] "
                                + m.embedded().text())
                        .collect(Collectors.joining("\n")))
                .build();
        log.debug("[ArabicSuggestionProcessor] Suggestion candidates:\n{}", cappedContext.getContext());

        String enrichedQuestion = request.getEnrichedQuestion() != null
                ? request.getEnrichedQuestion()
                : request.getRawQuestion();


        String question = suggestionService.suggestionProduct(enrichedQuestion, cappedContext, Language.ARABIC);
        String answer = chatModel.chat(question);

        log.debug("[ArabicSuggestionProcessor] Suggestion answer:\n{}", answer);

        List<String> matchedIds = matchedIdsResolver.resolve(answer, cappedContext, originalIntent);

        log.info("[ArabicSuggestionProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(originalIntent.getSemanticQuery())
                .type("suggest")
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }
}
