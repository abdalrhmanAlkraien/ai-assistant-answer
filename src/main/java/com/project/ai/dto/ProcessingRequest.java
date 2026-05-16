package com.project.ai.dto;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:06 PM
 */
@Getter
@Setter
@Builder
public class ProcessingRequest {

    private final Long userId;
    private final String rawQuestion;

    // populated by MemoryProcessor
    private String memoryContext;
    private String enrichedQuestion;

    // populated by IntentAnalyzer
    private SearchIntent searchIntent;

    // populated by SegmentProcessor
    private List<EmbeddingMatch<TextSegment>> vectorMatches;
    private List<EmbeddingMatch<TextSegment>> lastFilteredMatches;

//    public void setMemoryContext(String memoryContext) {
//        this.memoryContext = memoryContext;
//    }
//
//    public void setEnrichedQuestion(String enrichedQuestion) {
//        this.enrichedQuestion = enrichedQuestion;
//    }
//
//    public void setSearchIntent(SearchIntent searchIntent) {
//        this.searchIntent = searchIntent;
//    }
//
//    public void setVectorMatches(List<EmbeddingMatch<TextSegment>> vectorMatches) {
//        this.vectorMatches = vectorMatches;
//    }
}
