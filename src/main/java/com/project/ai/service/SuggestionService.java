package com.project.ai.service;

import com.project.ai.agents.Language;
import com.project.ai.config.PromptKeys;
import com.project.ai.loader.PromptLoader;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 12:55 AM
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class SuggestionService {

    private final PromptLoader promptLoader;

    public String suggestionProduct(final String question, final FilteredContext suggestContext, final Language language) {

        log.info("suggest product enable");

        String productsContext = suggestContext.getFilteredMatches().stream()
                .map(m -> "[" + m.embedded().metadata().getString("productId") + "] "
                        + m.embedded().text())
                .collect(Collectors.joining("\n"));

        String promptKey = language.equals(Language.ARABIC)
                ? PromptKeys.SUGGESTION_ARABIC
                : PromptKeys.SUGGESTION_ENGLISH;

        String template = promptLoader.get(promptKey);

        return template.formatted(question, productsContext);
    }

    public SearchIntent buildSuggestIntent(SearchIntent original) {
        // Relaxation order: remove brand first, then price, then category
        // Keep what's most important to the user, remove the most restrictive constraint

        // Step 1: Remove brand — keep category + price
        if (original.getBrand() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .minPrice(original.getMinPrice())
                    .maxPrice(original.getMaxPrice())
                    .semanticQuery(original.getSemanticQuery())
                    .build();
        }

        // Step 2: Remove price — keep category + brand
        if (original.getMaxPrice() != null || original.getMinPrice() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .brand(original.getBrand())
                    .semanticQuery(original.getSemanticQuery())
                    .build();
        }

        // Step 3: Remove category — keep brand + price
        if (original.getCategory() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .brand(original.getBrand())
                    .minPrice(original.getMinPrice())
                    .maxPrice(original.getMaxPrice())
                    .semanticQuery(original.getSemanticQuery())
                    .build();
        }

        // Step 4: All constraints removed — pure semantic
        return SearchIntent.builder()
                .searchType("suggest")
                .semanticQuery(original.getSemanticQuery())
                .build();
    }
}
