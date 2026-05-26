package com.project.ai.config;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 25/05/2026
 * @Time: 10:59 PM
 */
public final class PromptKeys {
    public static final String REQUEST_ANALYZER   = "request_analyzer";
    public static final String EXECUTION_PLANNER  = "execution_planner";
    public static final String MEMORY_ENRICHER    = "memory_enricher";
    public static final String SEGMENT_ENGLISH    = "segment_english";
    public static final String SEGMENT_ARABIC     = "segment_arabic";
    public static final String KNOWLEDGE_ENGLISH  = "knowledge_english";
    public static final String KNOWLEDGE_ARABIC   = "knowledge_arabic";
    public static final String SUGGESTION_ENGLISH = "suggestion_english";
    public static final String SUGGESTION_ARABIC  = "suggestion_arabic";
    public static final String SORT               = "sort";
    public static final String INTENT_ENGLISH     = "intent_english";     // ← add
    public static final String INTENT_ARABIC      = "intent_arabic";      // ← add

    public static final String SEGMENT_ENGLISH_PRICE      = "segment_english_price";
    public static final String SEGMENT_ENGLISH_CATEGORY   = "segment_english_category";
    public static final String SEGMENT_ENGLISH_BRAND      = "segment_english_brand";
    public static final String SEGMENT_ENGLISH_HYBRID     = "segment_english_hybrid";
    public static final String SEGMENT_ENGLISH_COMPARISON = "segment_english_comparison";
    public static final String SEGMENT_ENGLISH_SEMANTIC   = "segment_english_semantic";
    public static final String SEGMENT_ENGLISH_DEFAULT    = "segment_english_default";

    public static final String SEGMENT_ARABIC_PRICE      = "segment_arabic_price";
    public static final String SEGMENT_ARABIC_CATEGORY   = "segment_arabic_category";
    public static final String SEGMENT_ARABIC_BRAND      = "segment_arabic_brand";
    public static final String SEGMENT_ARABIC_HYBRID     = "segment_arabic_hybrid";
    public static final String SEGMENT_ARABIC_COMPARISON = "segment_arabic_comparison";
    public static final String SEGMENT_ARABIC_SEMANTIC   = "segment_arabic_semantic";
    public static final String SEGMENT_ARABIC_DEFAULT    = "segment_arabic_default";

    public static final String CLARIFICATION_ENGLISH = "clarification_english";
    public static final String CLARIFICATION_ARABIC  = "clarification_arabic";

    private PromptKeys() {}
}