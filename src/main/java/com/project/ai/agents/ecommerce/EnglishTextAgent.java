package com.project.ai.agents.ecommerce;

import com.project.ai.agents.AgentType;
import com.project.ai.agents.Language;
import com.project.ai.agents.MultimodalAgentStrategy;
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
            @Qualifier("ecommerceEnglishOrchestrator")
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

        log.info("[E-commerceEnglishTextAgent] START — userId ={}, question={}", request.getUserId(), request.getTextQuestion());

        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .userId(request.getUserId())
                .rawQuestion(request.getTextQuestion())
                .enrichedQuestion(request.getTextQuestion())  // already enriched by planner
                .tokenTracker(request.getTokenTracker())
                .memoryContext(request.getMemoryContext())
                .enrichmentDone(true)   // skip re-enrichment in orchestrator
                .intentDone(false)      // intent still needed in orchestrator
                .searchIntent(request.getSearchIntent())              // ← add this
                .relatedToPreviousContext(request.isRelatedToPreviousContext()) // ← add this
                .build();

        log.info("the memory is {}", request.getMemoryContext());

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
