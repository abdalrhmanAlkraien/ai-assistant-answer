package com.project.ai.dto;

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
}
