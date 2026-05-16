package com.project.ai.processing.text.structure;

import com.project.ai.dto.SearchIntent;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:24 PM
 */
public interface IntentAnalyzer {
    String enrichWithMemory(String question, String memoryContext);
    SearchIntent extractIntent(String userQuestion);
}
