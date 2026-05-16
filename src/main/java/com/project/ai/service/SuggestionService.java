package com.project.ai.service;

import com.project.ai.agents.Language;
import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.SearchIntent;
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
public class SuggestionService {

    public String suggestionProduct(final String question, final FilteredContext suggestContext, final Language language) {

        log.info("suggest product enable");

        String productsContext = suggestContext.getFilteredMatches().stream()
                .map(m -> "[" + m.embedded().metadata().getString("id") + "] "
                        + m.embedded().text())
                .collect(Collectors.joining("\n"));

        String suggestQuestion = "";
        if (language.equals(Language.ENGLISH)) {
            suggestQuestion = """
                    You are a helpful e-commerce assistant.
                    The user was looking for: "%s"
                    But no exact matches were found in our catalog.
                    
                    STRICT RULES:
                    - ONLY mention products from the list below — nothing else
                    - NEVER invent products not in this list
                    - Format response in a friendly helpful way
                    - Explain why this is a good alternative
                    - At the end list: "Product IDs: ..."
                    
                    Available alternatives:
                    %s
                    
                    Write a friendly suggestion explaining why these products are good alternatives:
                    """.formatted(question, productsContext);
        } else {

            suggestQuestion = """
                    أنت مساعد تسوق إلكتروني مفيد.
                    كان المستخدم يبحث عن: "%s"
                    لكن لم يتم العثور على تطابق دقيق في كتالوجنا.
                    
                    القواعد الصارمة:
                    - اذكر فقط المنتجات من القائمة أدناه — لا شيء غيرها
                    - لا تخترع منتجات غير موجودة في هذه القائمة أبداً
                    - اقترح فقط المنتجات ذات الصلة بما طلبه المستخدم
                    - فضّل المنتجات الأقرب لنطاق سعر المستخدم الأصلي
                    - إذا كان المنتج غير ذي صلة أو بعيداً جداً عن الميزانية، لا تذكره
                    - نسّق الرد بطريقة ودية ومفيدة
                    - اشرح لماذا هذا المنتج بديل جيد
                    - في النهاية اذكر: "معرفات المنتجات: ..."
                    
                    البدائل المتاحة:
                    %s
                    
                    اكتب اقتراحاً ودياً باللغة العربية يشرح لماذا هذه المنتجات بدائل جيدة:
                    
                    
                   تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(question, productsContext);
        }

        return suggestQuestion;
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
