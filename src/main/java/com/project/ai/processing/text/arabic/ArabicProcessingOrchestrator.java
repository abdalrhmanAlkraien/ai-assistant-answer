package com.project.ai.processing.text.arabic;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;
import com.project.ai.processing.text.structure.ProcessingOrchestrator;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:41 PM
 */

public interface ArabicProcessingOrchestrator extends ProcessingOrchestrator {

    @Override
    ProcessingResult process(final ProcessingRequest request);

    @Override
    ProcessingResult route(ProcessingRequest request);
}
