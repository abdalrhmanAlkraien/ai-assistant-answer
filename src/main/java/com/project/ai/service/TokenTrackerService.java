package com.project.ai.service;

import com.project.ai.dto.TokenDto;
import com.project.ai.dto.TokenTracker;
import com.project.ai.model.TokenCallRecord;
import com.project.ai.model.TokenRequestSummary;
import com.project.ai.repository.TokenCallRecordRepository;
import com.project.ai.repository.TokenRequestSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:33 AM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class TokenTrackerService {

    private final TokenRequestSummaryRepository requestRepo;
    private final TokenCallRecordRepository callRepo;

    @Transactional
    public TokenRequestSummary persist(TokenTracker tracker) {
        List<TokenTracker.CallEntry> entries = tracker.getEntries();

        int totalInput  = entries.stream().mapToInt(TokenTracker.CallEntry::inputTokens).sum();
        int totalOutput = entries.stream().mapToInt(TokenTracker.CallEntry::outputTokens).sum();

        TokenRequestSummary summary = TokenRequestSummary.builder()
                .requestId(tracker.getRequestId())
                .userId(tracker.getUserId())
                .modelName(tracker.getModelName())
                .userMessage(tracker.getUserMessage())
                .totalInputTokens(totalInput)
                .totalOutputTokens(totalOutput)
                .totalTokens(totalInput + totalOutput)
                .totalCalls(entries.size())
                .totalDurationMs(tracker.totalDurationMs())
                .createdAt(LocalDateTime.now())
                .build();

        List<TokenCallRecord> records = entries.stream()
                .map(e -> TokenCallRecord.builder()
                        .requestSummary(summary)
                        .callName(e.callName())
                        .inputTokens(e.inputTokens())
                        .outputTokens(e.outputTokens())
                        .totalTokens(e.totalTokens())
                        .durationMs(e.durationMs())
                        .calledAt(e.calledAt())
                        .build())
                .toList();

        summary.getCallRecords().addAll(records);
        requestRepo.save(summary);

        log.info("[Token] Persisted request={} user={} totalTokens={} calls={}",
                tracker.getRequestId(), tracker.getUserId(),
                summary.getTotalTokens(), entries.size());

        return summary;
    }

    // Global
    @Transactional(readOnly = true)
    public TokenDto.GlobalSummaryDto getGlobalSummary() {
        long totalRequests = requestRepo.countAllRequests();
        long totalInput    = nullSafe(requestRepo.sumAllInputTokens());
        long totalOutput   = nullSafe(requestRepo.sumAllOutputTokens());
        long grandTotal    = nullSafe(requestRepo.sumAllTotalTokens());

        List<TokenDto.CallAggregateDto> breakdown = mapCallAggregates(
                callRepo.aggregateByCallName()
        );

        return new TokenDto.GlobalSummaryDto(
                totalRequests, totalInput, totalOutput, grandTotal, breakdown
        );
    }


    @Transactional(readOnly = true)
    public TokenDto.UserSummaryDto getUserSummary(String userId) {
        long totalRequests = nullSafe(requestRepo.countRequestsByUserId(userId));
        long totalInput    = nullSafe(requestRepo.sumInputTokensByUserId(userId));
        long totalOutput   = nullSafe(requestRepo.sumOutputTokensByUserId(userId));
        long grandTotal    = nullSafe(requestRepo.sumTotalTokensByUserId(userId));

        List<TokenDto.CallAggregateDto> breakdown = mapCallAggregates(
                callRepo.aggregateByCallNameForUser(userId)
        );

        return new TokenDto.UserSummaryDto(
                userId, totalRequests, totalInput, totalOutput, grandTotal, breakdown
        );
    }

    @Transactional(readOnly = true)
    public Page<TokenDto.RequestSummaryDto> getAllRequests(Pageable pageable) {
        return requestRepo.findAll(pageable).map(this::toRequestDto);
    }

    // ── Single request ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenDto.RequestSummaryDto getByRequestId(String requestId) {
        return requestRepo.findByRequestIdWithCalls(requestId)
                .map(this::toRequestDto)
                .orElseThrow(() -> new IllegalArgumentException("Request not found: " + requestId));
    }

    // User
    @Transactional(readOnly = true)
    public Page<TokenDto.RequestSummaryDto> getUserRequests(String userId, Pageable pageable) {
        return requestRepo.findByUserId(userId, pageable).map(this::toRequestDto);
    }

    private TokenDto.RequestSummaryDto toRequestDto(TokenRequestSummary entity) {
        List<TokenDto.CallRecordDto> calls = entity.getCallRecords().stream()
                .map(c -> new TokenDto.CallRecordDto(
                        c.getCallName(),
                        c.getInputTokens(),
                        c.getOutputTokens(),
                        c.getTotalTokens(),
                        c.getDurationMs(),
                        c.getCalledAt()
                ))
                .toList();

        return new TokenDto.RequestSummaryDto(
                entity.getRequestId(),
                entity.getUserId(),
                entity.getModelName(),
                entity.getUserMessage(),
                entity.getTotalInputTokens(),
                entity.getTotalOutputTokens(),
                entity.getTotalTokens(),
                entity.getTotalCalls(),
                entity.getTotalDurationMs(),
                entity.getCreatedAt(),
                calls
        );
    }

    private List<TokenDto.CallAggregateDto> mapCallAggregates(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new TokenDto.CallAggregateDto(
                        (String)  row[0],
                        toLong(   row[1]),
                        toLong(   row[2]),
                        toLong(   row[3]),
                        toDouble( row[4]),
                        toLong(   row[5])
                ))
                .toList();
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private long toLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private double toDouble(Object o) {
        return o == null ? 0.0 : ((Number) o).doubleValue();
    }
}
