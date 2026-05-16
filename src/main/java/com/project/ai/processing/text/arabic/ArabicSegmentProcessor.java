package com.project.ai.processing.text.arabic;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:42 PM
 */
@Service
@Log4j2
public class ArabicSegmentProcessor implements ChatProcessor {

    private static final Set<String> SUPPORTED = Set.of(
            "semantic", "price", "category", "brand", "hybrid", "comparison", "sort");

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatModel chatModel;
    private final FilterProcessor filterProcessor;       // EnglishFilterProcessor via @Primary
    private final MatchedIdsResolver matchedIdsResolver; // EnglishMatchedIdsResolver via @Primary
    private final ChatProcessor suggestionProcessor;
    private final SearchService searchService;

    public ArabicSegmentProcessor(
            final EmbeddingStore<TextSegment> embeddingStore,
            @Qualifier("arabicChatModel") ChatModel chatModel,
            final FilterProcessor filterProcessor,
            final MatchedIdsResolver matchedIdsResolver,
            final ArabicSuggestionProcessor suggestionProcessor,
            final SearchService searchService
    ) {
        this.embeddingStore = embeddingStore;
        this.chatModel = chatModel;
        this.filterProcessor = filterProcessor;
        this.matchedIdsResolver = matchedIdsResolver;
        this.suggestionProcessor = suggestionProcessor;
        this.searchService = searchService;
    }

    @Override
    public boolean supports(String searchType) {
        return SUPPORTED.contains(searchType);
    }

    @Override
    public ProcessingResult process(ProcessingRequest request) {

        log.info("[ArabicSegmentProcessor] START — type={}", request.getSearchIntent().getSearchType());
        SearchIntent intent = request.getSearchIntent();

        log.info("[ArabicSegmentProcessor] Building search request — semantic='{}'",
                intent.getSemanticQuery());

        EmbeddingSearchRequest searchRequest = searchService.buildSearchRequest(intent);

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
        log.info("[ArabicSegmentProcessor] Vector search returned {} matches", matches.size());

        FilteredContext filteredContext = filterProcessor.filter(matches, intent);
        log.info("[ArabicSegmentProcessor] After filtering: {} products", filteredContext.getFilteredMatches().size());
        log.debug("[ArabicSegmentProcessor] Filtered products:\n{}", filteredContext.getContext());

        if (filteredContext.getFilteredMatches().isEmpty()) {
            log.info("[ArabicSegmentProcessor] No products after filtering — delegating to SuggestionProcessor");
            request.setVectorMatches(matches);
            return suggestionProcessor.process(request);
        }

        String prompt = buildPrompt(intent, filteredContext,
                request.getRawQuestion(), request.getMemoryContext());

        log.debug("[ArabicSegmentProcessor] Prompt sent to LLM:\n{}", prompt);

        String answer = chatModel.chat(prompt);
        log.debug("[ArabicSegmentProcessor] LLM answer:\n{}", answer);

        List<String> matchedIds = matchedIdsResolver.resolve(answer, filteredContext, intent);
        log.info("[ArabicSegmentProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(answer)
                .matchedIds(matchedIds)
                .build();
    }


    private String buildPrompt(final SearchIntent intent,
                               final FilteredContext filteredContext,
                               final String userQuestion,
                               final String memoryContext) {

        String memorySection = memoryContext == null || memoryContext.isEmpty() ? "" : """
                سجل المحادثة مع المستخدم:
                %s
                
                """.formatted(memoryContext);

        String context = filteredContext.getContext();
        int count = filteredContext.getFilteredMatches().size();


        return switch (intent.getSearchType()) {
            case "price" -> """
                    أنت مساعد لعرض قوائم المنتجات.
                    قام النظام بتصفية %d منتج لك مسبقاً.
                    مهمتك الوحيدة هي سرد جميع المنتجات البالغ عددها %d. لا تتخطَّ أياً منها.
                    
                    - ابدأ ردك بـ "إليك المنتجات ضمن النطاق السعري المطلوب:" ثم اعرض القائمة
                    المنتجات:
                    %s
                    
                    %s
                    اسرد جميع المنتجات البالغ عددها %d، كل منتج في سطر بهذا التنسيق (لا تطبع هذا السطر):
                    اسم المنتج - السعر - الفئة
                    
                    تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(count, count, context, memorySection, count);

            case "category" -> """
                    أنت مساعد لعرض قوائم المنتجات.
                    المستخدم يبحث عن منتجات: "%s".
                    
                    راجع المنتجات أدناه وأدرج فقط المنتجات ذات الصلة بـ "%s".
                    القواعد:
                    - أدرج المنتجات التي تنتمي فئتها بالضبط أو كفئة فرعية من "%s"
                    - أدرج المنتجات التي وصفها أو علاماتها مرتبطة بوضوح بـ "%s"
                    - استبعد المنتجات غير ذات الصلة بـ "%s"
                    - مهمتك هي عرض القائمة فقط — لا تعلّق على سجل المحادثة
                    - احتفظ بأسماء المنتجات والأسعار كما هي بالإنجليزية
                    - ابدأ ردك بـ "إليك المنتجات المتاحة:" ثم اعرض القائمة
                    
                    المنتجات:
                    %s
                    
                    %s
                    اسرد المنتجات ذات الصلة، كل منتج في سطر بهذا التنسيق (لا تطبع هذا السطر):
                    اسم المنتج - السعر - الفئة
                    تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(), intent.getCategory(),
                    intent.getCategory(),
                    context, memorySection);

            case "brand" -> """
                    أنت مساعد لعرض قوائم المنتجات.
                    المستخدم يبحث عن منتجات من هذه العلامات التجارية: "%s".
                    
                    راجع المنتجات أدناه وأدرج فقط المنتجات التي تنتمي لأي من هذه العلامات التجارية.
                    إذا كان المنتج ينتمي بوضوح لعلامة تجارية مختلفة، استبعده.
                    - ابدأ ردك بـ "إليك منتجات العلامة التجارية المطلوبة:" ثم اعرض القائمة
                    المنتجات:
                    %s
                    
                    %s
                    اسرد المنتجات ذات الصلة، كل منتج في سطر بهذا التنسيق (لا تطبع هذا السطر):
                    اسم المنتج - السعر - الفئة
                    
                    تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(intent.getBrand(), context, memorySection);

            case "hybrid" -> """
                    أنت مساعد لتصفية المنتجات. صفّ المنتجات وفق جميع هذه المعايير:
                    %s%s%s
                    
                    أدرج فقط المنتجات المطابقة لجميع المعايير. استبعد كل ما عداها.
                    - ابدأ ردك بـ "إليك المنتجات المطابقة للمعايير:" ثم اعرض القائمة
                    المنتجات:
                    %s
                    
                    %s
                    اسرد المنتجات المطابقة، كل منتج في سطر بهذا التنسيق (لا تطبع هذا السطر):
                    اسم المنتج - السعر - الفئة
                    تذكر: الإجابة باللغة العربية فقط.
                    """.formatted(
                    intent.getMinPrice() != null ? "- السعر >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- السعر <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- العلامة التجارية: " + intent.getBrand() + "\n" : "",
                    context, memorySection);

            case "comparison" -> """
                    أنت مساعد تسوق إلكتروني مفيد.
                    قارن المنتجات أدناه وأجب على سؤال المستخدم مباشرة.
                    كن مختصراً — أعطِ إجابة واضحة أو حدد الفائز.
                    أجب دائماً باللغة العربية.
                    في النهاية اذكر: "معرفات المنتجات: ..."
                    
                    المنتجات:
                    %s
                    
                    %s
                    سؤال المستخدم: %s
                    
                    الإجابة:
                   
                   
                    تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(context, memorySection, userQuestion);

            case "semantic" -> """
                    أنت مساعد تسوق إلكتروني مفيد.
                    المستخدم يبحث عن: "%s"
                    أجب بناءً فقط على المنتجات المدرجة أدناه.
                    %s%s
                    القواعد:
                    - أوصِ فقط بالمنتجات ذات الصلة بطلب المستخدم
                    - %s
                    - أعطِ الأولوية للمنتج الأكثر ملاءمة لحالة الاستخدام، وليس الأرخص
                    - كن مختصراً واشرح لماذا يناسب كل منتج الطلب
                    - أجب دائماً باللغة العربية
                    - في النهاية اذكر: "معرفات المنتجات: ..."
                    
                    المنتجات:
                    %s
                    
                    %sالإجابة:
                    
                    
                   تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(
                    intent.getSemanticQuery(),
                    intent.getMaxPrice() != null ? "- الحد الأقصى للسعر: $" + intent.getMaxPrice() + "\n" : "",
                    intent.getCategory() != null ? "- الفئة: " + intent.getCategory() + "\n" : "",
                    intent.getMaxPrice() != null
                            ? "التزم بحد السعر — لا تُوصِ بمنتجات تتجاوز $" + intent.getMaxPrice()
                            : "لا يوجد قيد على السعر — أوصِ بأفضل منتج لحالة الاستخدام بغض النظر عن السعر",
                    context, memorySection);

            default -> """
                    أنت مساعد تسوق إلكتروني مفيد.
                    أجب على سؤال المستخدم بناءً فقط على المنتجات المدرجة أدناه.
                    كل منتج يبدأ بمعرّفه بين قوسين معقوفين.
                    كن مختصراً ومفيداً.
                    أجب دائماً باللغة العربية.
                    في النهاية اذكر معرفات المنتجات التي ذكرتها: "معرفات المنتجات: ..."
                    
                    المنتجات:
                    %s
                    
                    %s
                    سؤال المستخدم: %s
                    
                    الإجابة:
                    
                    تذكر: الإجابة باللغة العربية فقط.
                    
                    """.formatted(context, memorySection, userQuestion);
        };
    }
}
