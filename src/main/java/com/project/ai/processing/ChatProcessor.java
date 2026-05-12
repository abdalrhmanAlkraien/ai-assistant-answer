package com.project.ai.processing;

import com.project.ai.dto.ProcessingRequest;
import com.project.ai.dto.ProcessingResult;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 12/05/2026
 * @Time: 9:05 PM
 */
public interface ChatProcessor {
    boolean supports(String searchType);
    ProcessingResult process(ProcessingRequest request);
}
