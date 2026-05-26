package com.project.ai.processing.text.english;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.text.structure.ProcessingOrchestrator;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:18 PM
 */
public interface EnglishProcessingOrchestrator extends ProcessingOrchestrator {

    @Override
    ProcessingResult process(final ProcessingRequest request);

    @Override
    ProcessingResult route(ProcessingRequest request);
}
