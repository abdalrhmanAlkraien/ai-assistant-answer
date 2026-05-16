package com.project.ai.util;

import com.project.ai.agents.Language;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:06 PM
 */
@UtilityClass
@Log4j2
public class LanguageDetector {

    private static final double ARABIC_THRESHOLD = 0.3;

    public Language detect(String text) {
        if (text == null || text.isBlank()) {
            log.info("[LanguageDetector] empty text — defaulting to ENGLISH");
            return Language.ENGLISH;
        }

        long arabicChars = text.chars()
                .filter(c -> c >= 0x0600 && c <= 0x06FF)
                .count();

        double arabicRatio = (double) arabicChars / text.length();
        Language detected = arabicRatio > ARABIC_THRESHOLD
                ? Language.ARABIC
                : Language.ENGLISH;

        log.info("[LanguageDetector] text='{}', arabicRatio={:.2f}, detected={}",
                text.length() > 50 ? text.substring(0, 50) + "..." : text,
                arabicRatio,
                detected);

        return detected;
    }
}
