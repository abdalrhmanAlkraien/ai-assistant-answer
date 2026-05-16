package com.project.ai.processing.text.structure;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 10:30 PM
 */
public interface ProcessingOrchestrator {
    ProcessingResult process(final ProcessingRequest request);
    ProcessingResult route(ProcessingRequest request);
}
