package com.project.ai.controller;

import com.project.ai.dto.TokenDto;
import com.project.ai.service.TokenTrackerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Analytics — Tokens", description = "Token usage tracking and analytics per user and globally")
public class TokenTrackingController {

    private final TokenTrackerService tokenTrackerService;


    @Operation(
            summary = "Global token summary",
            description = "Overall stats: total tokens consumed, total requests, and breakdown per call type"
    )
    @ApiResponse(responseCode = "200", description = "Global summary retrieved")
    @GetMapping("/summary")
    public ResponseEntity<TokenDto.GlobalSummaryDto> getGlobalSummary() {
        return ResponseEntity.ok(tokenTrackerService.getGlobalSummary());
    }

    @Operation(
            summary = "All requests (paginated)",
            description = "Paginated list of all token requests across all users. Default sort by createdAt."
    )
    @ApiResponse(responseCode = "200", description = "Requests retrieved")
    @GetMapping("/requests")
    public ResponseEntity<Page<TokenDto.RequestSummaryDto>> getAllRequests(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(tokenTrackerService.getAllRequests(pageable));
    }

    @Operation(
            summary = "Get request by ID",
            description = "Full detail for one request including every LLM call breakdown and token counts"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request found"),
            @ApiResponse(responseCode = "404", description = "Request not found", content = @Content)
    })
    @GetMapping("/requests/{requestId}")
    public ResponseEntity<TokenDto.RequestSummaryDto> getByRequestId(
            @Parameter(description = "Request UUID", required = true)
            @PathVariable String requestId) {
        return ResponseEntity.ok(tokenTrackerService.getByRequestId(requestId));
    }

    @Operation(
            summary = "User token summary",
            description = "Aggregated token stats for a specific user — total tokens, total requests, average per request"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User summary retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<TokenDto.UserSummaryDto> getUserSummary(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId) {
        return ResponseEntity.ok(tokenTrackerService.getUserSummary(userId));
    }

    @Operation(
            summary = "User request history (paginated)",
            description = "Paginated list of all requests made by a specific user with token usage per request"
    )
    @ApiResponse(responseCode = "200", description = "User requests retrieved")
    @GetMapping("/users/{userId}/requests")
    public ResponseEntity<Page<TokenDto.RequestSummaryDto>> getUserRequests(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(tokenTrackerService.getUserRequests(userId, pageable));
    }
}
