package com.project.ai.service;

import com.project.ai.config.LangChain4jProperties;
import com.project.ai.dto.SearchIntent;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 14/05/2026
 * @Time: 11:29 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class SearchService {

    private final EmbeddingModel embeddingModel;
    private final LangChain4jProperties properties;

    public EmbeddingSearchRequest buildSearchRequest(SearchIntent intent) {

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
            case "semantic", "brand", "knowledge", "comparison", "sort" -> false;
            default -> false;
        };
    }
}
