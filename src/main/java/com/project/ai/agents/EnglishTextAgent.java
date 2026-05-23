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
public class EnglishTextAgent implements MultimodalAgentStrategy {

    private final ProcessingOrchestrator orchestrator;

    public EnglishTextAgent(
            @Qualifier("englishProcessingOrchestrator")
            ProcessingOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public boolean supports(MultimodalRequest request) {
        return AgentType.TEXT.name().equals(request.getInputType().name())
                && Language.ENGLISH.name().equals(request.getDetectedLanguage().name());
    }

    @Override
    public MultimodalResponse process(MultimodalRequest request) {

        log.info("[EnglishTextAgent] START — userId ={}, question={}", request.getUserId(), request.getTextQuestion());

        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .userId(request.getUserId())
                .rawQuestion(request.getTextQuestion())
                .tokenTracker(request.getTokenTracker())
                .build();

        ProcessingResult result = orchestrator.process(processingRequest);

        return MultimodalResponse.builder()
                .question(result.getEnrichedQuestion())
                .type(result.getType())
                .answer(result.getAnswer())
                .matchProducts(result.getMatchedIds())
                .language(Language.ENGLISH)
                .inputType(InputType.TEXT)
                .responseTime(LocalDateTime.now())
                .build();
    }
}
