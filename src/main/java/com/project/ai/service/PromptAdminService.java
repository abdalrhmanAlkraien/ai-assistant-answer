package com.project.ai.service;

import com.project.ai.dto.PromptCreateRequest;
import com.project.ai.dto.PromptDetailDto;
import com.project.ai.dto.PromptStatsDto;
import com.project.ai.dto.PromptSummaryDto;
import com.project.ai.loader.PromptLoader;
import com.project.ai.model.prompt.BusinessPrompt;
import com.project.ai.repository.BusinessPromptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 1:15 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class PromptAdminService {

    private final BusinessPromptRepository promptRepository;
    private final PromptLoader promptLoader;

    @Value("${app.business-strategy}") String businessName;


    @Transactional
    public PromptDetailDto createPrompt(PromptCreateRequest request) {
        log.info("[PromptAdminService] Creating prompt key={} business={}",
                request.getPromptKey(), businessName);

        // check for duplicate key in same business
        promptRepository.findByBusinessNameAndPromptKey(businessName, request.getPromptKey())
                .ifPresent(p -> {
                    throw new IllegalArgumentException(
                            "Prompt already exists for key='" + request.getPromptKey()
                                    + "' business='" + businessName + "'");
                });

        BusinessPrompt prompt = BusinessPrompt.builder()
                .businessName(businessName)
                .promptKey(request.getPromptKey())
                .promptTemplate(request.getPromptTemplate())
                .version(1)
                .active(request.isActive())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BusinessPrompt saved = promptRepository.save(prompt);

        // load into cache if active
        if (request.isActive()) {
            promptLoader.reload(saved.getPromptKey());
            log.info("[PromptAdminService] Loaded new prompt key={} into cache", saved.getPromptKey());
        }

        log.info("[PromptAdminService] Created prompt id={} key={}", saved.getId(), saved.getPromptKey());

        return PromptDetailDto.builder()
                .id(saved.getId())
                .businessName(saved.getBusinessName())
                .promptKey(saved.getPromptKey())
                .promptTemplate(saved.getPromptTemplate())
                .version(saved.getVersion())
                .isActive(saved.isActive())
                .createdAt(saved.getCreatedAt())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }


    public void updatePrompt(String promptKey, String newPromptTemplate) {

        log.info("[PromptAdminService] Updating prompt key='{}'", promptKey);

        promptRepository.findByBusinessNameAndPromptKeyAndActiveTrue(
                        businessName, promptKey)
                .ifPresent(p -> {
                    p.setPromptTemplate(newPromptTemplate);
                    p.setUpdatedAt(LocalDateTime.now());
                    promptRepository.save(p);
                });

        promptLoader.reload(promptKey);
    }

    public void reloadAll() {
        promptLoader.reloadAll();
    }

    public Page<PromptSummaryDto> getAllPrompts(Pageable pageable) {
        log.info("[PromptAdminService] Fetching all prompts");
        return promptRepository.findAll(pageable)
                .map(p -> PromptSummaryDto.builder()
                        .id(p.getId())
                        .businessName(p.getBusinessName())
                        .promptKey(p.getPromptKey())
                        .version(p.getVersion())
                        .isActive(p.isActive())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build());
    }

    public PromptDetailDto getPromptById(Long id) {
        log.info("[PromptAdminService] Fetching prompt id={}", id);
        BusinessPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found for id: " + id));

        return PromptDetailDto.builder()
                .id(prompt.getId())
                .businessName(prompt.getBusinessName())
                .promptKey(prompt.getPromptKey())
                .promptTemplate(prompt.getPromptTemplate())
                .version(prompt.getVersion())
                .isActive(prompt.isActive())
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }


    @Transactional
    public PromptSummaryDto toggleStatus(Long id, boolean active) {
        log.info("[PromptAdminService] Toggling prompt id={} active={}", id, active);

        BusinessPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found for id: " + id));

        prompt.setActive(active);
        prompt.setUpdatedAt(LocalDateTime.now());
        BusinessPrompt saved = promptRepository.saveAndFlush(prompt);  // ← use saveAndFlush

        log.info("[PromptAdminService] After update — id={} active={}", id, saved.isActive());

        // reload cache if activating
        if (active) {
            promptLoader.reload(prompt.getPromptKey());
            log.info("[PromptAdminService] Reloaded prompt key={} after activation", prompt.getPromptKey());
        }

        log.info("[PromptAdminService] Prompt id={} key={} active={}", id, prompt.getPromptKey(), active);

        return PromptSummaryDto.builder()
                .id(prompt.getId())
                .businessName(prompt.getBusinessName())
                .promptKey(prompt.getPromptKey())
                .version(prompt.getVersion())
                .isActive(prompt.isActive())
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }

    @Transactional
    public void deletePrompt(Long id) {
        log.info("[PromptAdminService] Deleting prompt id={}", id);

        BusinessPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found for id: " + id));

        promptRepository.delete(prompt);
        log.info("[PromptAdminService] Deleted prompt id={} key={}", id, prompt.getPromptKey());
    }

    @Transactional
    public void deletePrompts(List<Long> ids) {
        log.info("[PromptAdminService] Batch deleting {} prompts", ids.size());

        List<Long> existing = promptRepository.findAllByIdIn(ids)
                .stream()
                .map(BusinessPrompt::getId)
                .toList();

        List<Long> notFound = ids.stream()
                .filter(id -> !existing.contains(id))
                .toList();

        if (!notFound.isEmpty()) {
            log.warn("[PromptAdminService] Prompt IDs not found — skipping: {}", notFound);
        }

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("None of the provided prompt IDs exist: " + ids);
        }

        promptRepository.deleteAllByIdIn(existing);
        log.info("[PromptAdminService] Deleted {} prompts", existing.size());
    }

    public PromptStatsDto getStats() {
        log.info("[PromptAdminService] Fetching prompt stats for business={}", businessName);

        Long total = promptRepository.countByBusinessName(businessName);
        Long active = promptRepository.countByBusinessNameAndActive(businessName, true);
        Long inactive = promptRepository.countByBusinessNameAndActive(businessName, false);

        return PromptStatsDto.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .build();
    }
}
