package com.project.ai.dto;

import com.project.ai.model.Product;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 08/05/2026
 * @Time: 9:11 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilteredContext {

    private String context;
    private List<EmbeddingMatch<TextSegment>> filteredMatches;
    private List<com.project.ai.model.Product> products;    // ← add for SQL results

    // helper — works for both vector and SQL
    public boolean isEmpty() {
        boolean vectorEmpty = filteredMatches == null || filteredMatches.isEmpty();
        boolean sqlEmpty = products == null || products.isEmpty();
        return vectorEmpty && sqlEmpty;
    }

    public List<String> getMatchedIds() {
        // SQL results take priority
        if (products != null && !products.isEmpty()) {
            return products.stream()
                    .map(Product::getProductId)
                    .toList();
        }
        // fallback to vector match IDs
        if (filteredMatches != null) {
            return filteredMatches.stream()
                    .map(m -> m.embedded().metadata().getString("id"))
                    .toList();
        }
        return List.of();
    }
}
