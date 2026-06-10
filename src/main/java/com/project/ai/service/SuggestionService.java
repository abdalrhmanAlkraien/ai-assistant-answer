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

        // preserve price ceiling from original intent — never relax it
        Double priceCeiling = original.getMaxSuggestPrice() != null
                ? original.getMaxSuggestPrice()
                : original.getMaxPrice();  // first call — copy maxPrice as ceiling

        // Step 1: Remove brand — keep category + price, remember excluded brand
        if (original.getBrand() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .maxPrice(priceCeiling)        // ← keep price ceiling
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(original.getBrand())
                    .maxSuggestPrice(priceCeiling) // ← carry ceiling
                    .build();
        }

        // Step 2: Remove price — keep category
        if (original.getMaxPrice() != null || original.getMinPrice() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .maxPrice(priceCeiling)        // ← keep price ceiling
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(original.getExcludedBrand())
                    .maxSuggestPrice(priceCeiling) // ← carry ceiling
                    .build();
        }

        // Step 3: Remove category
        if (original.getCategory() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .maxPrice(priceCeiling)        // ← keep price ceiling
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(original.getExcludedBrand())
                    .maxSuggestPrice(priceCeiling) // ← carry ceiling
                    .build();
        }

        // Step 4: Pure semantic
        return SearchIntent.builder()
                .searchType("suggest")
                .maxPrice(priceCeiling)
                .semanticQuery(original.getSemanticQuery())
                .build();
    }}
