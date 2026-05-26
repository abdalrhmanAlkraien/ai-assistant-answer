package com.project.ai.processing.text.structure;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 16/05/2026
 * @Time: 11:13 PM
 */
public interface MemoryContext {

//    void prepareContext(final ProcessingRequest request);
    void saveToMemory(final ProcessingRequest request, ProcessingResult result);
}
