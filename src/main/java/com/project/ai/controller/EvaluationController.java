package com.project.ai.controller;

import com.project.ai.dto.evels.ContextDetailDto;
import com.project.ai.dto.evels.ContextPageDto;
import com.project.ai.dto.evels.EvaluationDetailDto;
import com.project.ai.dto.evels.EvaluationPageDto;
import com.project.ai.dto.evels.EvaluationSummaryDto;
import com.project.ai.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 1:44 AM
 */
@RestController
@RequestMapping("/api/growth/evals")
@RequiredArgsConstructor
@Log4j2
public class EvaluationController {

    private final EvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<Page<EvaluationPageDto>> getEvaluations(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String triggeredType,
            @RequestParam(required = false) String language,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                evaluationService.getEvaluations(status, triggeredType, language, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EvaluationDetailDto> getEvaluationById(@PathVariable Long id) {
        return ResponseEntity.ok(evaluationService.getEvaluationById(id));
    }

    @GetMapping("/summary/last")
    public ResponseEntity<EvaluationSummaryDto> getLastEvaluationSummary() {
        return ResponseEntity.ok(evaluationService.getLastEvaluationSummary());
    }

    @GetMapping("/{id}/contexts")
    public ResponseEntity<Page<ContextPageDto>> getContexts(
            @PathVariable Long id,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String searchType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(
                evaluationService.getContexts(id, language, status, searchType, pageable));
    }

    @GetMapping("/{id}/contexts/{contextId}")
    public ResponseEntity<ContextDetailDto> getContextDetail(
            @PathVariable Long id,
            @PathVariable Long contextId) {

        return ResponseEntity.ok(evaluationService.getContextDetail(id, contextId));
    }
}
