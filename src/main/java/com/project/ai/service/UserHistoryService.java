package com.project.ai.service;

import com.project.ai.dto.HistoryStatsDto;
import com.project.ai.dto.UserHistorySummary;
import com.project.ai.dto.UserMessageDto;
import com.project.ai.model.ConversationMemory;
import com.project.ai.repository.ConversationMemoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 11:45 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class UserHistoryService {

    private final ConversationMemoryRepository memoryRepository;

    public Page<UserHistorySummary> getAllUsersWithStats(Pageable pageable) {
        log.info("[UserHistoryService] Fetching all users with message stats");

        return memoryRepository.findAllUsersWithStats(pageable)
                .map(row -> UserHistorySummary.builder()
                        .userId(((Number) row[0]).longValue())
                        .messageCount(((Number) row[1]).longValue())
                        .lastActivity((LocalDateTime) row[2])
                        .build());
    }

    public Page<UserMessageDto> getUserHistory(Long userId, Pageable pageable) {
        log.info("[UserHistoryService] Fetching history for userId={}", userId);

        return memoryRepository.findByUserIdOrderByCreatedAtAsc(userId, pageable)
                .map(cm -> {
                    List<String> products = new ArrayList<>();
                    if(cm.getMatchedProducts() != null) {
                        products = Arrays.stream(cm.getMatchedProducts()).toList();
                    }
                    return UserMessageDto.builder()
                            .id(cm.getId())
                            .role(cm.getRole().name().toLowerCase())
                            .content(cm.getMessage())
                            .searchType(cm.getSearchType())
                            .matchedProducts(products)
                            .createdAt(cm.getCreatedAt())
                            .build();
                });
    }

    @Transactional
    public void deleteMessages(Long userId, List<Long> messageIds) {
        log.info("[UserHistoryService] Deleting {} messages for userId={}", messageIds.size(), userId);

        // validate messages belong to this user
        List<Long> existing = memoryRepository
                .findByUserIdAndIdIn(userId, messageIds)
                .stream()
                .map(ConversationMemory::getId)
                .toList();

        List<Long> notFound = messageIds.stream()
                .filter(id -> !existing.contains(id))
                .toList();

        if (!notFound.isEmpty()) {
            log.warn("[UserHistoryService] Messages not found for userId={} — skipping: {}", userId, notFound);
        }

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("None of the provided message IDs exist for userId: " + userId);
        }

        memoryRepository.deleteByUserIdAndIdIn(userId, existing);
        log.info("[UserHistoryService] Deleted {} messages for userId={}", existing.size(), userId);
    }

    @Transactional
    public void clearUserHistory(Long userId) {
        log.info("[UserHistoryService] Clearing all history for userId={}", userId);

        if (!memoryRepository.existsByUserId(userId)) {
            log.info("[UserHistoryService] No history found for userId: {} — skipping", userId);
            return;
        }

        memoryRepository.deleteAllByUserId(userId);
        log.info("[UserHistoryService] Cleared all history for userId={}", userId);
    }

    public HistoryStatsDto getStats() {
        log.info("[UserHistoryService] Fetching history stats");
        return HistoryStatsDto.builder()
                .totalUsers(memoryRepository.countDistinctUsers())
                .totalRecords(memoryRepository.countBy())
                .build();
    }
}
