package com.project.ai.controller;

import com.project.ai.dto.TokenDto;
import com.project.ai.service.TokenTrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 1:42 AM
 */
@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
public class TokenTrackingController {

    private final TokenTrackerService tokenTrackerService;

    // ── Global ────────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/tokens/summary
     * Overall stats: total tokens, total requests, breakdown per call type.
     */
    @GetMapping("/summary")
    public ResponseEntity<TokenDto.GlobalSummaryDto> getGlobalSummary() {
        return ResponseEntity.ok(tokenTrackerService.getGlobalSummary());
    }

    /**
     * GET /api/v1/tokens/requests?page=0&size=20&sort=createdAt,desc
     * Paginated list of all requests.
     */
    @GetMapping("/requests")
    public ResponseEntity<Page<TokenDto.RequestSummaryDto>> getAllRequests(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(tokenTrackerService.getAllRequests(pageable));
    }


    /**
     * GET /api/v1/tokens/requests/{requestId}
     * Full detail for one request including every call breakdown.
     */
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<TokenDto.RequestSummaryDto> getByRequestId(
            @PathVariable String requestId
    ) {
        return ResponseEntity.ok(tokenTrackerService.getByRequestId(requestId));
    }

    // ── Per user ──────────────────────────────────────────────────────────────

    /**
     * GET /api/v1/tokens/users/{userId}/summary
     * Aggregated token stats for one user.
     */
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<TokenDto.UserSummaryDto> getUserSummary(
            @PathVariable String userId
    ) {
        return ResponseEntity.ok(tokenTrackerService.getUserSummary(userId));
    }

    /**
     * GET /api/v1/tokens/users/{userId}/requests?page=0&size=20
     * Paginated request history for one user.
     */
    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<Page<TokenDto.RequestSummaryDto>> getUserRequests(
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        return ResponseEntity.ok(tokenTrackerService.getUserRequests(userId, pageable));
    }
}
