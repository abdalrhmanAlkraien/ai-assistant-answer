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

    public String suggestionProduct(String question, FilteredContext context, Language language) {
        String productsContext = context.getProducts().stream()
                .map(p -> "[" + p.getProductId() + "] " + p.getTitle()
                        + " - " + p.getPrice()
                        + " - " + p.getCategory()
                        + " - " + p.getDescription())
                .collect(Collectors.joining("\n"));

        String promptKey = language.equals(Language.ARABIC)
                ? PromptKeys.SUGGESTION_ARABIC
                : PromptKeys.SUGGESTION_ENGLISH;

        String template = promptLoader.get(promptKey);
        return template.formatted(question, productsContext);
    }

    public SearchIntent buildSuggestIntent(SearchIntent original) {

        Double priceCeiling = original.getMaxSuggestPrice() != null
                ? original.getMaxSuggestPrice()
                : original.getMaxPrice();

        // always carry excludedBrand from original — never lose it
        String excludedBrand = original.getExcludedBrand() != null
                ? original.getExcludedBrand()
                : original.getBrand();  // first call — set from brand

        // Step 1: Remove brand — keep category + price
        if (original.getBrand() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .maxPrice(priceCeiling)
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(excludedBrand)    // ← always set
                    .maxSuggestPrice(priceCeiling)
                    .build();
        }

        // Step 2: Remove price — keep category
        if (original.getMaxPrice() != null || original.getMinPrice() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .category(original.getCategory())
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(excludedBrand)    // ← always carry
                    .maxSuggestPrice(priceCeiling)
                    .build();
        }

        // Step 3: Remove category
        if (original.getCategory() != null) {
            return SearchIntent.builder()
                    .searchType("suggest")
                    .semanticQuery(original.getSemanticQuery())
                    .excludedBrand(excludedBrand)    // ← always carry
                    .maxSuggestPrice(priceCeiling)
                    .build();
        }

        // Step 4: Pure semantic — still exclude original brand
        return SearchIntent.builder()
                .searchType("suggest")
                .semanticQuery(original.getSemanticQuery())
                .excludedBrand(excludedBrand)        // ← always carry
                .build();
    }
}
