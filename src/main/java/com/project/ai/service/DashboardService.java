package com.project.ai.service;

import com.project.ai.dto.DashboardStatsDto;
import com.project.ai.dto.LanguageDistributionDto;
import com.project.ai.dto.RecentRequestDto;
import com.project.ai.dto.RequestsByTypeDto;
import com.project.ai.dto.ResponseTimeTrendDto;
import com.project.ai.repository.BusinessPromptRepository;
import com.project.ai.repository.ConversationMemoryRepository;
import com.project.ai.repository.ProductRepository;
import com.project.ai.repository.TokenRequestSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 11:50 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class DashboardService {

    private final TokenRequestSummaryRepository tokenRequestSummaryRepository;
    private final ProductRepository productRepository;
    private final BusinessPromptRepository promptRepository;
    private final ConversationMemoryRepository memoryRepository;

    @Value("${app.business-strategy}")
    private String businessName;

    public DashboardStatsDto getStats() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();  // ← today at 00:00:00

        return DashboardStatsDto.builder()
                .requestsToday(tokenRequestSummaryRepository.countRequestsToday(startOfDay))
                .requestsTotal(tokenRequestSummaryRepository.countBy())
                .activeUsersToday(tokenRequestSummaryRepository.countActiveUsersToday(startOfDay))
                .totalProducts(productRepository.countByActiveTrue())
                .activePrompts(promptRepository.countByBusinessNameAndActive(businessName, true))
                .avgResponseTimeMs(tokenRequestSummaryRepository.avgResponseTimeMs())
                .tokensToday(tokenRequestSummaryRepository.sumTokensToday(startOfDay))
                .tokensTotal(tokenRequestSummaryRepository.sumTokensTotal())
                .build();
    }

    public List<RequestsByTypeDto> getRequestsByType() {
        log.info("[DashboardService] Fetching requests by type");

        return memoryRepository.countBySearchType()
                .stream()
                .map(row -> RequestsByTypeDto.builder()
                        .type((String) row[0])
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<LanguageDistributionDto> getLanguageDistribution() {
        log.info("[DashboardService] Fetching language distribution");

        return memoryRepository.countByLanguage()
                .stream()
                .map(row -> LanguageDistributionDto.builder()
                        .language(row[0] != null ? row[0].toString() : "UNKNOWN")  // ← use toString() instead of cast
                        .count(((Number) row[1]).longValue())
                        .build())
                .toList();
    }

    public List<ResponseTimeTrendDto> getResponseTimeTrend(int days) {
        log.info("[DashboardService] Fetching response time trend for last {} days", days);

        LocalDateTime from = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();

        return tokenRequestSummaryRepository.findResponseTimeTrend(from)
                .stream()
                .map(row -> ResponseTimeTrendDto.builder()
                        .date(row[0].toString())
                        .avgResponseTimeMs(((Number) row[1]).doubleValue())
                        .build())
                .toList();
    }

    public List<RecentRequestDto> getRecentRequests(int size) {
        log.info("[DashboardService] Fetching recent {} requests", size);

        Pageable pageable = PageRequest.of(0, size);

        return tokenRequestSummaryRepository.findRecentRequests(pageable)
                .stream()
                .map(t -> RecentRequestDto.builder()
                        .userId(t.getUserId())
                        .question(t.getUserMessage())
                        .type(resolveType(t.getUserMessage()))
                        .language(memoryRepository
                                .findLastLanguageByUserId(t.getUserId())
                                .orElse("UNKNOWN"))
                        .responseTimeMs(t.getTotalDurationMs())
                        .createdAt(t.getCreatedAt())
                        .build())
                .toList();
    }

    private String resolveType(String userMessage) {
        if (userMessage == null) return "unknown";
        // type is not stored in token summary — return unknown
        // frontend can use requests-by-type endpoint for type breakdown
        return "unknown";
    }
}
