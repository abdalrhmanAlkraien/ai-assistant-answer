package com.project.ai.service;

import com.project.ai.dto.PromptCreateRequest;
import com.project.ai.dto.PromptDetailDto;
import com.project.ai.dto.PromptStatsDto;
import com.project.ai.dto.PromptSummaryDto;
import com.project.ai.dto.prompt.PromptRollbackResponse;
import com.project.ai.dto.prompt.PromptUpdateRequest;
import com.project.ai.dto.prompt.PromptVersionDto;
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

        if (promptRepository.existsByBusinessNameAndPromptKeyAndActiveTrue(
                businessName, request.getPromptKey())) {
            throw new IllegalArgumentException(
                    "Active prompt already exists for key='" + request.getPromptKey() + "'");
        }

        int nextVersion = promptRepository.findMaxVersionByBusinessNameAndPromptKey(
                businessName, request.getPromptKey()) + 1;

        BusinessPrompt prompt = BusinessPrompt.builder()
                .businessName(businessName)
                .promptKey(request.getPromptKey())
                .promptTemplate(request.getPromptTemplate())
                .version(nextVersion)
                .active(request.isActive())
                .description(request.getDescription())
                .updatedBy(request.getUpdatedBy())
                .changeReason(request.getChangeReason())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BusinessPrompt saved = promptRepository.save(prompt);

        if (request.isActive()) {
            promptLoader.reload(saved.getPromptKey());
            log.info("[PromptAdminService] Loaded new prompt key={} into cache", saved.getPromptKey());
        }

        log.info("[PromptAdminService] Created prompt id={} key={} version={}",
                saved.getId(), saved.getPromptKey(), saved.getVersion());

        return toDetailDto(saved);
    }

    @Transactional
    public PromptDetailDto updatePromptVersion(String promptKey, PromptUpdateRequest request) {
        log.info("[PromptAdminService] Creating new version for key='{}'", promptKey);

        // deactivate current active version
        promptRepository.findByBusinessNameAndPromptKeyAndActiveTrue(businessName, promptKey)
                .ifPresent(current -> {
                    current.setActive(false);
                    current.setDeactivatedAt(LocalDateTime.now());
                    current.setUpdatedAt(LocalDateTime.now());
                    promptRepository.save(current);
                    log.info("[PromptAdminService] Deactivated version={} for key='{}'",
                            current.getVersion(), promptKey);
                });

        // calculate next version
        int nextVersion = promptRepository.findMaxVersionByBusinessNameAndPromptKey(
                businessName, promptKey) + 1;

        // create new version
        BusinessPrompt newVersion = BusinessPrompt.builder()
                .businessName(businessName)
                .promptKey(promptKey)
                .promptTemplate(request.getPromptTemplate())
                .version(nextVersion)
                .active(true)
                .description(request.getDescription())
                .updatedBy(request.getUpdatedBy())
                .changeReason(request.getChangeReason())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BusinessPrompt saved = promptRepository.save(newVersion);

        // reload cache
        promptLoader.reload(promptKey);

        log.info("[PromptAdminService] Created version={} for key='{}'",
                saved.getVersion(), promptKey);

        return toDetailDto(saved);
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

    @Transactional
    public PromptRollbackResponse rollback(String promptKey) {
        log.info("[PromptAdminService] Rolling back prompt key='{}'", promptKey);

        // find current active
        BusinessPrompt current = promptRepository
                .findByBusinessNameAndPromptKeyAndActiveTrue(businessName, promptKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No active prompt found for key='" + promptKey + "'"));

        // find previous version
        BusinessPrompt previous = promptRepository
                .findPreviousVersion(businessName, promptKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No previous version found for key='" + promptKey + "'"));

        int fromVersion = current.getVersion();
        int toVersion   = previous.getVersion();

        // deactivate current
        current.setActive(false);
        current.setDeactivatedAt(LocalDateTime.now());
        current.setUpdatedAt(LocalDateTime.now());
        promptRepository.save(current);

        // activate previous
        previous.setActive(true);
        previous.setDeactivatedAt(null);
        previous.setUpdatedAt(LocalDateTime.now());
        promptRepository.save(previous);

        // reload cache
        promptLoader.reload(promptKey);

        log.info("[PromptAdminService] Rolled back key='{}' from v{} to v{}",
                promptKey, fromVersion, toVersion);

        return PromptRollbackResponse.builder()
                .promptKey(promptKey)
                .rolledBackFromVersion(fromVersion)
                .rolledBackToVersion(toVersion)
                .message("Successfully rolled back '" + promptKey
                        + "' from v" + fromVersion + " to v" + toVersion)
                .build();
    }

    public List<PromptVersionDto> getHistory(String promptKey) {
        log.info("[PromptAdminService] Fetching history for key='{}'", promptKey);

        return promptRepository
                .findAllByBusinessNameAndPromptKeyOrderByVersionDesc(businessName, promptKey)
                .stream()
                .map(this::toVersionDto)
                .toList();
    }

    public void reloadAll() {
        promptLoader.reloadAll();
    }

    public Page<PromptSummaryDto> getAllPrompts(String promptKey, Boolean active, Pageable pageable) {
        log.info("[PromptAdminService] Fetching prompts — promptKey={} active={}", promptKey, active);
        return promptRepository
                .findAllWithFilters(businessName, promptKey, active, pageable)
                .map(this::toSummaryDto);
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
                .description(prompt.getDescription())
                .evalRunAt(prompt.getEvalRunAt())
                .evalScore(prompt.getEvalScore())
                .changeReason(prompt.getChangeReason())
                .deactivatedAt(prompt.getDeactivatedAt())
                .createdAt(prompt.getCreatedAt())
                .updatedAt(prompt.getUpdatedAt())
                .build();
    }


    @Transactional
    public PromptSummaryDto toggleStatus(Long id, boolean active) {
        log.info("[PromptAdminService] Toggling prompt id={} active={}", id, active);

        BusinessPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found for id: " + id));

        // block activation if another version is already active
        if (active && !prompt.isActive()) {
            boolean anotherActive = promptRepository
                    .existsByBusinessNameAndPromptKeyAndActiveTrue(
                            businessName, prompt.getPromptKey());
            if (anotherActive) {
                throw new IllegalStateException(
                        "Cannot activate version=" + prompt.getVersion()
                                + " — another version of '" + prompt.getPromptKey()
                                + "' is already active. Use rollback instead.");
            }
        }

        // deactivation — allowed, just set deactivatedAt
        if (!active && prompt.isActive()) {
            prompt.setDeactivatedAt(LocalDateTime.now());
            // evict from cache when deactivating
            promptLoader.evict(prompt.getPromptKey());  // ← add this
            log.info("[PromptAdminService] Evicted key={} from cache after deactivation", prompt.getPromptKey());
        }

        prompt.setActive(active);
        prompt.setUpdatedAt(LocalDateTime.now());
        BusinessPrompt saved = promptRepository.saveAndFlush(prompt);

        if (active) {
            promptLoader.reload(prompt.getPromptKey());
            log.info("[PromptAdminService] Reloaded prompt key={} after activation", prompt.getPromptKey());
        }

        log.info("[PromptAdminService] Prompt id={} key={} active={}",
                id, prompt.getPromptKey(), active);

        return toSummaryDto(saved);
    }

    @Transactional
    public void deletePrompt(Long id) {
        log.info("[PromptAdminService] Deleting prompt id={}", id);

        BusinessPrompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found for id: " + id));

        promptRepository.delete(prompt);

        // if deleted prompt was active — evict from cache
        if (prompt.isActive()) {
            promptLoader.evict(prompt.getPromptKey());
            log.info("[PromptAdminService] Evicted key={} from cache after delete", prompt.getPromptKey());
        }

        log.info("[PromptAdminService] Deleted prompt id={} key={}", id, prompt.getPromptKey());
    }

    @Transactional
    public void deletePrompts(List<Long> ids) {
        log.info("[PromptAdminService] Batch deleting {} prompts", ids.size());

        List<BusinessPrompt> existing = promptRepository.findAllByIdIn(ids);

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("None of the provided prompt IDs exist: " + ids);
        }

        List<Long> notFound = ids.stream()
                .filter(id -> existing.stream().noneMatch(p -> p.getId().equals(id)))
                .toList();

        if (!notFound.isEmpty()) {
            log.warn("[PromptAdminService] Prompt IDs not found — skipping: {}", notFound);
        }

        // evict active ones from cache before deleting
        existing.stream()
                .filter(BusinessPrompt::isActive)
                .forEach(p -> {
                    promptLoader.evict(p.getPromptKey());
                    log.info("[PromptAdminService] Evicted key={} from cache", p.getPromptKey());
                });

        promptRepository.deleteAllByIdIn(existing.stream().map(BusinessPrompt::getId).toList());
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

    // ── Mappers ───────────────────────────────────────────────────────────────

    private PromptDetailDto toDetailDto(BusinessPrompt p) {
        return PromptDetailDto.builder()
                .id(p.getId())
                .businessName(p.getBusinessName())
                .promptKey(p.getPromptKey())
                .promptTemplate(p.getPromptTemplate())
                .version(p.getVersion())
                .isActive(p.isActive())
                .description(p.getDescription())
                .updatedBy(p.getUpdatedBy())
                .changeReason(p.getChangeReason())
                .evalScore(p.getEvalScore())
                .evalRunAt(p.getEvalRunAt())
                .deactivatedAt(p.getDeactivatedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PromptSummaryDto toSummaryDto(BusinessPrompt p) {
        return PromptSummaryDto.builder()
                .id(p.getId())
                .businessName(p.getBusinessName())
                .promptKey(p.getPromptKey())
                .version(p.getVersion())
                .isActive(p.isActive())
                .description(p.getDescription())
                .updatedBy(p.getUpdatedBy())
                .changeReason(p.getChangeReason())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }

    private PromptVersionDto toVersionDto(BusinessPrompt p) {
        return PromptVersionDto.builder()
                .id(p.getId())
                .promptKey(p.getPromptKey())
                .version(p.getVersion())
                .isActive(p.isActive())
                .description(p.getDescription())
                .updatedBy(p.getUpdatedBy())
                .changeReason(p.getChangeReason())
                .evalScore(p.getEvalScore())
                .evalRunAt(p.getEvalRunAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deactivatedAt(p.getDeactivatedAt())
                .build();
    }
}
