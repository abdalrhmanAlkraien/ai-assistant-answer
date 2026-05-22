package com.project.ai.processing.text.arabic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.dto.SearchIntent;
import com.project.ai.processing.text.structure.IntentAnalyzer;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:15 PM
 */
@Service
@Log4j2
public class ArabicIntentAnalyzer implements IntentAnalyzer {

    private final ChatModel chatModel;
    private final ObjectMapper mapper;

    public ArabicIntentAnalyzer(
            @Qualifier("arabicChatModel") ChatModel chatModel,
            ObjectMapper mapper) {
        this.chatModel = chatModel;
        this.mapper = mapper;
    }

    @Override
    public SearchIntent extractIntent(String userQuestion) {

        log.debug("[ArabicIntentAnalyzer] Raw intent JSON:\n{}", userQuestion);

        String priceInstruction = getPriceInstruction(userQuestion);

        String intentPrompt = """
                حلل سؤال المستخدم واستخرج فلاتر البحث بتنسيق JSON فقط.
                أعد فقط هذا الهيكل JSON، لا شيء آخر، بدون markdown أو backticks:
                {
                  "searchType": "semantic | price | category | brand | hybrid | knowledge | comparison | suggest | sort",
                  "minPrice": null or number,
                  "maxPrice": null or number,
                  "category": null or string in English,
                  "brand": null or string,
                  "semanticQuery": "the cleaned search query in English — for vector search only",
                  "semanticQueryArabic": ""the cleaned search query in Arabic — for response only",
                  "sortDirection": null or "asc" or "desc"
                }
                
                %s
                أنواع البحث:
                - "price"      → المستخدم يسأل عن نطاق سعري فقط
                - "category"   → المستخدم يسأل عن فئة منتج
                - "brand"      → المستخدم يريد رؤية منتجات من علامة تجارية معينة
                - "hybrid"     → مزيج من فلترين أو أكثر (علامة تجارية + سعر، فئة + سعر، إلخ)
                - "semantic"   → المستخدم يسأل عن توصيات أو أفضل منتج لحالة استخدام معينة
                - "knowledge"  → المستخدم يسأل سؤالاً عاماً أو كيفية عمل شيء ما (لا حاجة لبحث عن منتج)
                - "comparison" → المستخدم يريد مقارنة منتجات محددة أو إيجاد الأرخص/الأفضل
                                 استخرج العلامة التجارية فقط إذا ذُكرت صراحةً في السؤال
                                 لا تستخرج الفئة أبداً من أسئلة المقارنة — اضبط category على null دائماً
                                 sortDirection يجب أن يكون null دائماً للمقارنة
                - "suggest"    → المستخدم يطلب بدائل أو منتجات مشابهة
                - "sort"       → المستخدم يريد ترتيب المنتجات المعروضة حسب السعر أو الاسم
                                 استخرج الفئة والعلامة التجارية من استعلام الترتيب إذا وُجدا
                                 "رتب اللابتوبات تصاعدياً" → category: "laptops", sortDirection: "asc"
                                 "رتب هواتف سامسونج تنازلياً" → brand: "Samsung", sortDirection: "desc"
                                         ╔══════════════════════════════════════════════════════════════╗
                                         ║  قواعد اللغة — إلزامية بدون استثناء                        ║
                                         ║  "category"          → إنجليزية فقط                        ║
                                         ║  "semanticQuery"     → إنجليزية فقط  (vector search)       ║
                                         ║  "semanticQueryArabic" → عربية فصحى فقط  (LLM prompt)     ║
                                         ║  "brand"             → كما هي (Samsung, Apple ...)         ║
                                         ╚══════════════════════════════════════════════════════════════╝
                
                        جدول ترجمة الفئات — استخدم هذه القيم حرفياً في حقل category:
                        لابتوب / لاب توب / حاسوب محمول  → "laptops"
                        لابتوب ألعاب / جيمينج لابتوب   → "gaming laptops"
                        جوال / هاتف / موبايل / تليفون   → "smartphones"
                        سماعات رأس                     → "headphones"
                        سماعات أذن / إيربودز            → "earbuds"
                        مكبر صوت / سبيكر               → "speakers"
                        تلفزيون / شاشة تلفاز            → "tvs"
                        ألعاب فيديو / كونسول            → "gaming"
                        ساعة ذكية                      → "wearables"
                        كاميرا                         → "cameras"
                        إكسسوارات                      → "accessories"
                        منزل ذكي                       → "smart home"
                        تخزين                          → "storage"
                        شاشة كمبيوتر / مونيتور         → "monitors"
                        مطبخ                           → "kitchen"
                        أجهزة منزلية                   → "appliances"
                        أحذية                          → "shoes"
                        ملابس                          → "clothing"
                مهم جداً:
                
                - أعد category و semanticQuery دائماً باللغة العربية بغض النظر عن لغة السؤال
                - ترجمة الفئات: لابتوب/حاسوب محمول = laptop، جوال/هاتف/موبايل = smartphone،
                  سماعات = headphones، ساعة ذكية = smartwatch، تلفزيون = TV
                - ترجمة الأسعار: أقل من = under/less than، أكثر من = more than، بين = between
                - أسماء العلامات التجارية: احتفظ بها كما هي (سامسونج = Samsung، آبل = Apple، شاومي = Xiaomi)
                
                لنوع "sort": اضبط sortDirection على "asc" للتصاعدي/الأرخص أولاً، "desc" للتنازلي/الأغلى أولاً.
                لنوع "comparison": اضبط sortDirection على null دائماً، و category على null دائماً.
                لجميع الأنواع الأخرى: اضبط sortDirection على null.
                
                أمثلة:
                "قارن آيفون مع سامسونج"                    → knowledge
                "ما الفرق بين OLED و QLED؟"                → knowledge
                "اعرض لي منتجات سامسونج"                   → brand
                "أفضل لابتوب للألعاب"                      → semantic
                "أيهما أرخص؟"                              → comparison, brand: null, category: null, sortDirection: null
                "أيهما أرخص من سامسونج؟"                   → comparison, brand: "Samsung", category: null, sortDirection: null
                "أيهما أفضل قيمة؟"                         → comparison, brand: null, category: null, sortDirection: null
                "ما الفرق في السعر؟"                        → comparison, brand: null, category: null, sortDirection: null
                "منتجات بين 100 و 500 دولار"               → price
                "اعرض لي لابتوبات تحت 500 دولار"           → hybrid
                "اعرض لي لابتوبات سامسونج تحت 500 دولار"   → hybrid
                "رتب اللابتوبات تصاعدياً"                  → sort, category: "laptops", sortDirection: "asc"
                "رتب هواتف سامسونج تنازلياً"               → sort, brand: "Samsung", sortDirection: "desc"
                "الأرخص أولاً"                             → sort, sortDirection: "asc"
                "الأغلى أولاً"                             → sort, sortDirection: "desc"
                "اعرض بدائل"                               → suggest
                "أعطني شيئاً مشابهاً"                      → suggest
                
                سؤال المستخدم: %s
                """.formatted(priceInstruction, userQuestion);

        String intentJson = chatModel.chat(intentPrompt);
        log.debug("[ArabicIntentAnalyzer] Raw intent JSON:\n{}", intentJson);

        try {
            // clean markdown backticks if LLM adds them
            String cleaned = intentJson
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return mapper.readValue(cleaned, SearchIntent.class);

        } catch (JsonProcessingException e) {
            log.warn("[ArabicIntentAnalyzer] Failed to parse intent, falling back to pure semantic search: {}", e.getMessage());
            // fallback — treat as pure semantic search
            return SearchIntent.builder()
                    .searchType("semantic")
                    .semanticQuery(userQuestion)
                    .build();
        }
    }

    @NotNull
    private static String getPriceInstruction(String userQuestion) {
        boolean ignorePriceHint = userQuestion.matches(
                ".*\\b(without.*price|ignore.*price|no.*budget|any.*price|" +
                        "regardless.*price|price.*matter|don.t care.*price|" +
                        "بدون.*سعر|تجاهل.*سعر|أي.*سعر|بغض.*النظر.*سعر|" +
                        "ما يهمنيش.*سعر|مهما.*كان.*سعر|السعر.*مش.*مهم)\\b.*");

        String priceInstruction = ignorePriceHint
                ? "- المستخدم قال صراحةً تجاهل السعر — اضبط minPrice و maxPrice على null\n"
                : "";
        return priceInstruction;
    }

    @Override
    public String enrichWithMemory(String question, String memoryContext) {

        log.info("[ArabicIntentAnalyzer] enrichWithMemory — question='{}'", question);
        log.debug("[ArabicIntentAnalyzer] Memory context for enrichment:\n{}", memoryContext);

        if (memoryContext == null || memoryContext.isBlank()) {
            log.info("[ArabicIntentAnalyzer] - Memory context is blank");
            return question;
        }

        String prompt = """
                بناءً على سجل المحادثة التالي:
                %s
                
                يسأل المستخدم الآن: "%s"
                
                أولاً: حدد نوع الطلب:
                1. عملية — ترتيب، تصاعدي، تنازلي، الأرخص أولاً، الأغلى أولاً
                   (sort, order, ascending, descending, cheapest first, most expensive first)
                2. توصية — أفضل، ينصح، مناسب لـ، أيهما أشتري، ما الأفضل
                   (best, recommend, good for, suitable for, which one should I buy)
                3. مقارنة — أيهما أرخص، أيهما أفضل، الفرق في السعر، أغلى، قارن
                   (which is cheaper, which is better, price difference, more expensive)
                4. عرض قائمة — اعرض لي، قائمة، ما هي، أعطني
                   (show me, list, what are, give me)
                5. موضوع جديد — المستخدم يطرح فئة أو نوع منتج جديد تماماً
                6. تضييق الفلتر — المستخدم يضيف قيد سعر أو علامة تجارية أو فئة على نتائج سابقة
                
                القواعد:
                
                إذا كان العملية (ترتيب/تصاعدي/تنازلي):
                - انظر إلى آخر بحث عن منتج في السجل
                - احتفظ فقط بالفئة أو العلامة التجارية من آخر بحث
                - لا تعود إلى عمليات بحث سابقة في المحادثة
                - مثال للمخرج: "sort laptops ascending"، "sort Samsung smartphones descending"
                
                إذا كان توصية (أفضل/مناسب لـ/أيهما أشتري):
                - استخدم "best" أو "أفضل" في إعادة الصياغة — لا تستخدم "show me" أو "اعرض لي" أبداً
                - احتفظ بالفئة وقيود السعر من آخر بحث
                - إذا قال المستخدم تجاهل السعر / السعر مش مهم / بغض النظر عن السعر → احذف قيد السعر
                - مثال للمخرج: "best laptop for gaming under $1000"، "best laptop for gaming"
                
                إذا كان مقارنة (أيهما أرخص/أفضل/الفرق في السعر):
                - لا تحوّل إلى توصية ("best X") أبداً — احتفظ بنية المقارنة
                - لا تضف قيود سعر من سياق سابق غير ذي صلة
                - اشر فقط إلى المنتجات من آخر بحث
                - استخدم "which is cheaper"، "compare"، "what is the price difference" في إعادة الصياغة
                - مثال للمخرج: "which is cheaper, iPhone 15 Pro or Samsung Galaxy S24?"
                
                إذا كان عرض قائمة (اعرض/قائمة/أعطني):
                - استخدم "show me" في إعادة الصياغة
                - احتفظ بالفئة من السجل إذا كان السؤال غامضاً
                - مثال للمخرج: "show me laptops under $1000"، "show me Samsung smartphones"
                
                إذا كان موضوعاً جديداً:
                - احتفظ بالموضوع الجديد كما هو — لا تستبدله بموضوع من السجل
                - مثال للمخرج: "show me smartphones" (حتى لو كان آخر بحث عن لابتوبات)
                
                إذا كان تضييق فلتر (تحت X / علامة تجارية فقط / إضافة قيد):
                - احتفظ بالفئة/العلامة التجارية من آخر بحث
                - أضف القيد الجديد
                - احتفظ بجميع القيم الرقمية بالضبط — لا تستبدلها بـ "رخيص" أو "بأسعار معقولة"
                - مثال للمخرج: "show me laptops under $1000"
                
                مهم جداً:
                - أعد دائماً السؤال المُعاد صياغته باللغة الإنجليزية
                - ترجم الفئات إلى الإنجليزية: لابتوب = laptop، جوال/هاتف = smartphone،
                  سماعات = headphones، ساعة ذكية = smartwatch، تلفزيون = TV
                - احتفظ بأسماء العلامات التجارية كما هي: سامسونج = Samsung، آبل = Apple
                - احتفظ بالأرقام والأسعار بالضبط كما ذكرها المستخدم
                
                أمثلة:
                السجل: "المستخدم بحث عن لابتوبات → HP Pavilion، MacBook، Dell XPS"
                السؤال: "رتبها تصاعدياً"
                صحيح: "sort laptops ascending"  ✅
                خطأ:  "sort products ascending"  ❌
                
                السجل: "المستخدم بحث عن هواتف سامسونج → Galaxy S24"
                السؤال: "رتب تنازلياً"
                صحيح: "sort Samsung smartphones descending"  ✅
                
                السجل: "المستخدم كان يبحث عن لابتوبات تحت 1000 دولار"
                السؤال: "ما الأفضل للألعاب؟"
                صحيح: "best laptop for gaming under $1000"  ✅
                خطأ:  "show me laptops under $1000 for gaming"  ❌
                
                السجل: "المستخدم كان يبحث عن لابتوبات تحت 1000 دولار"
                السؤال: "ما الأفضل للألعاب بغض النظر عن السعر؟"
                صحيح: "best laptop for gaming"  ✅
                خطأ:  "best laptop for gaming under $1000"  ❌
                
                السجل: "المستخدم كان يبحث عن لابتوبات"
                السؤال: "ما الذي تحت 1000؟"
                صحيح: "show me laptops under $1000"  ✅
                خطأ:  "products under $1000"  ❌
                
                السجل: "مقارنة آيفون مع سامسونج"
                السؤال: "اعرض لي لابتوبات"
                صحيح: "show me laptops"  ✅
                خطأ:  "show me iPhone and Samsung laptops"  ❌
                
                السجل: "المستخدم بحث عن لابتوبات"
                السؤال: "أيهما أشتري للعمل؟"
                صحيح: "best laptop for work"  ✅
                خطأ:  "show me laptops for work"  ❌
                
                السجل: "المستخدم وجد iPhone 15 Pro (999 دولار) و Samsung Galaxy S24 (899 دولار)"
                السؤال: "أيهما أرخص؟"
                صحيح: "which is cheaper, iPhone 15 Pro or Samsung Galaxy S24?"  ✅
                خطأ:  "best smartphone under $1000"  ❌
                
                السجل: "المستخدم بحث عن هواتف سامسونج → Galaxy S24 (899 دولار)"
                السؤال: "أيهما أرخص؟"
                صحيح: "which Samsung smartphone is cheaper?"  ✅
                خطأ:  "best smartphone under $1000"  ❌
                
                أعد فقط السؤال المُعاد صياغته باللغة العربية، لا شيء آخر.
                """.formatted(memoryContext, question);

        try {
            String enriched = chatModel.chat(prompt);
            log.info("[ArabicIntentAnalyzer] enriched: '{}'", enriched);
            return enriched.trim();
        } catch (Exception e) {
            log.warn("[ArabicIntentAnalyzer] enrichment failed: {}", e.getMessage());
            return question;
        }
    }
}
