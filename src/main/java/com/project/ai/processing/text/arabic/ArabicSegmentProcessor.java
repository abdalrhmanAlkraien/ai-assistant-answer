package com.project.ai.processing.text.arabic;

import com.project.ai.dto.FilteredContext;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.dto.SearchIntent;
import com.project.ai.dto.TokenTracker;
import com.project.ai.processing.ChatProcessor;
import com.project.ai.processing.text.structure.FilterProcessor;
import com.project.ai.processing.text.structure.MatchedIdsResolver;
import com.project.ai.service.SearchService;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
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

    private static final String LANGUAGE_SYSTEM_HEADER = """
        أنت مساعد تسوق إلكتروني. تحدث العربية الفصحى فقط.
        قواعد غير قابلة للكسر:
        - استخدم اللغة العربية الفصحى في جميع ردودك دون استثناء
        - ممنوع تمامًا استخدام الروسية أو الصينية أو أي لغة أخرى
        - أسماء المنتجات والأرقام والعلامات التجارية تُكتب كما هي بالإنجليزية فقط
        - إذا لم تجد كلمة عربية مناسبة، استخدم التعريب
        
        """;

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

        TokenTracker tracker = request.getTokenTracker();

        long intentStart = System.currentTimeMillis();

        ChatResponse answer = chatModel.chat(UserMessage.from(prompt));

        long intentDuration = System.currentTimeMillis() - intentStart;

        tracker.record(
                "arabic-segment-processor",
                answer.tokenUsage().inputTokenCount(),
                answer.tokenUsage().outputTokenCount(),
                intentDuration
        );

        log.debug("[ArabicSegmentProcessor] LLM answer:\n{}", answer.aiMessage().text());

        List<String> matchedIds = matchedIdsResolver.resolve(answer.aiMessage().text(), filteredContext, intent);
        log.info("[ArabicSegmentProcessor] END — matchedIds={}", matchedIds);

        return ProcessingResult.builder()
                .enrichedQuestion(intent.getSemanticQuery())
                .type(intent.getSearchType())
                .answer(answer.aiMessage().text())
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


        String body = switch (intent.getSearchType()) {

            case "price" -> """
                مهمتك: سرد جميع المنتجات البالغ عددها %d ضمن النطاق السعري المطلوب.
                لا تتخطَّ أي منتج. قام النظام بالتصفية مسبقًا.
                
                ابدأ ردك بـ: "إليك المنتجات ضمن النطاق السعري المطلوب:"
                
                المنتجات (%d منتج):
                %s
                
                %s
                اسرد جميع المنتجات بهذا التنسيق:
                اسم المنتج - السعر - الفئة
                
                """.formatted(count, count, context, memorySection);

            case "category" -> """
                مهمتك: عرض المنتجات المرتبطة بـ "%s" فقط.
                
                القواعد:
                - أدرج المنتجات التي فئتها أو وصفها مرتبط بـ "%s"
                - استبعد المنتجات غير ذات الصلة تمامًا
                - احتفظ بأسماء المنتجات والأسعار كما هي
                
                ابدأ ردك بـ: "إليك المنتجات المتاحة:"
                
                المنتجات:
                %s
                
                %s
                اسرد المنتجات ذات الصلة بهذا التنسيق:
                اسم المنتج - السعر - الفئة
                
                """.formatted(
                    intent.getCategory(), intent.getCategory(),
                    context, memorySection);

            case "brand" -> """
                مهمتك: عرض منتجات العلامات التجارية: "%s" فقط.
                استبعد أي منتج ينتمي لعلامة تجارية مختلفة.
                
                ابدأ ردك بـ: "إليك منتجات العلامة التجارية المطلوبة:"
                
                المنتجات:
                %s
                
                %s
                اسرد المنتجات ذات الصلة بهذا التنسيق:
                اسم المنتج - السعر - الفئة
                
                """.formatted(intent.getBrand(), context, memorySection);

            case "hybrid" -> """
                مهمتك: تصفية المنتجات وفق جميع هذه المعايير معًا:
                %s%s%s
                أدرج فقط المنتجات المطابقة لجميع المعايير.
                
                ابدأ ردك بـ: "إليك المنتجات المطابقة للمعايير:"
                
                المنتجات:
                %s
                
                %s
                اسرد المنتجات المطابقة بهذا التنسيق:
                اسم المنتج - السعر - الفئة
                
                """.formatted(
                    intent.getMinPrice() != null ? "- السعر >= $" + intent.getMinPrice() + "\n" : "",
                    intent.getMaxPrice() != null ? "- السعر <= $" + intent.getMaxPrice() + "\n" : "",
                    intent.getBrand() != null ? "- العلامة التجارية: " + intent.getBrand() + "\n" : "",
                    context, memorySection);

            case "comparison" -> """
                مهمتك: مقارنة المنتجات أدناه والإجابة على سؤال المستخدم مباشرة.
                كن مختصرًا — إجابة واضحة أو حدد الفائز.
                في النهاية اذكر: "معرفات المنتجات: ..."
                
                المنتجات:
                %s
                
                %s
                سؤال المستخدم: %s
                
                الإجابة:
                """.formatted(context, memorySection, userQuestion);

            case "semantic" -> """
                مهمتك: التوصية بالمنتجات المناسبة لطلب المستخدم.
                طلب المستخدم: "%s"
                %s%s
                القواعد:
                - أوصِ فقط بالمنتجات ذات الصلة بالطلب
                - %s
                - أعطِ الأولوية للأنسب للاستخدام وليس الأرخص
                - اشرح بإيجاز لماذا يناسب كل منتج الطلب
                - في النهاية اذكر: "معرفات المنتجات: ..."
                
                المنتجات:
                %s
                
                %s
                الإجابة:
                """.formatted(
                    intent.getSemanticQuery(),
                    intent.getMaxPrice() != null ? "- الحد الأقصى للسعر: $" + intent.getMaxPrice() + "\n" : "",
                    intent.getCategory() != null ? "- الفئة: " + intent.getCategory() + "\n" : "",
                    intent.getMaxPrice() != null
                            ? "التزم بحد السعر — لا تُوصِ بمنتجات تتجاوز $" + intent.getMaxPrice()
                            : "لا يوجد قيد على السعر — أوصِ بأفضل منتج بغض النظر عن السعر",
                    context, memorySection);

            default -> """
                مهمتك: الإجابة على سؤال المستخدم بناءً على المنتجات أدناه فقط.
                كن مختصرًا ومفيدًا.
                في النهاية اذكر: "معرفات المنتجات: ..."
                
                المنتجات:
                %s
                
                %s
                سؤال المستخدم: %s
                
                الإجابة:
                """.formatted(context, memorySection, userQuestion);
        };

        return LANGUAGE_SYSTEM_HEADER + body;
    }
}
