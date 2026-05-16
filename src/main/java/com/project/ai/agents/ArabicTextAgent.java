package com.project.ai.agents;

import com.project.ai.dto.InputType;
import com.project.ai.dto.MultimodalRequest;
import com.project.ai.dto.MultimodalResponse;
import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.text.structure.ProcessingOrchestrator;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:07 PM
 */
@Service
@Log4j2
public class ArabicTextAgent implements MultimodalAgentStrategy {

    private final ProcessingOrchestrator orchestrator;

    public ArabicTextAgent(
            @Qualifier("arabicProcessingOrchestrator")
            ProcessingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public boolean supports(MultimodalRequest request) {
        return request.getInputType() == InputType.TEXT
                && request.getDetectedLanguage() == Language.ARABIC;
    }

    @Override
    public MultimodalResponse process(MultimodalRequest request) {
        log.info("[ArabicAgentOrchestrator] START — userId={}", request.getUserId());

        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .userId(request.getUserId())
                .rawQuestion(request.getNormalizedText())
                .build();

        ProcessingResult result = orchestrator.process(processingRequest);

        log.info("[ArabicAgentOrchestrator] END — type={}", result.getType());

        return MultimodalResponse.builder()
                .question(result.getEnrichedQuestion())
                .type(result.getType())
                .answer(result.getAnswer())
                .matchProducts(result.getMatchedIds())
                .language(Language.ARABIC)
                .inputType(InputType.TEXT)
                .responseTime(LocalDateTime.now())
                .build();
    }
}
