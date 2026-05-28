package com.project.ai.controller;

import com.project.ai.dto.DashboardStatsDto;
import com.project.ai.dto.LanguageDistributionDto;
import com.project.ai.dto.RecentRequestDto;
import com.project.ai.dto.RequestsByTypeDto;
import com.project.ai.dto.ResponseTimeTrendDto;
import com.project.ai.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 28/05/2026
 * @Time: 11:51 PM
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "Dashboard", description = "Dashboard stats and analytics")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "Get dashboard stats",
            description = "Returns key metrics: requests today/total, active users, products, prompts, tokens, avg response time"
    )
    @ApiResponse(responseCode = "200", description = "Stats retrieved")
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @Operation(
            summary = "Requests by type",
            description = "Returns count of all requests grouped by search type — category, semantic, comparison, sort etc."
    )
    @ApiResponse(responseCode = "200", description = "Data retrieved")
    @GetMapping("/requests-by-type")
    public ResponseEntity<List<RequestsByTypeDto>> getRequestsByType() {
        return ResponseEntity.ok(dashboardService.getRequestsByType());
    }

    @Operation(
            summary = "Language distribution",
            description = "Returns conversation count grouped by language — ARABIC vs ENGLISH"
    )
    @ApiResponse(responseCode = "200", description = "Data retrieved")
    @GetMapping("/language-distribution")
    public ResponseEntity<List<LanguageDistributionDto>> getLanguageDistribution() {
        return ResponseEntity.ok(dashboardService.getLanguageDistribution());
    }

    @Operation(
            summary = "Response time trend",
            description = "Returns average response time per day for the last N days. Default 14 days."
    )
    @ApiResponse(responseCode = "200", description = "Trend data retrieved")
    @GetMapping("/response-time-trend")
    public ResponseEntity<List<ResponseTimeTrendDto>> getResponseTimeTrend(
            @Parameter(description = "Number of days to look back", example = "14")
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(dashboardService.getResponseTimeTrend(days));
    }

    @Operation(
            summary = "Recent requests",
            description = "Returns the most recent N requests with user, question, type, language and response time"
    )
    @ApiResponse(responseCode = "200", description = "Recent requests retrieved")
    @GetMapping("/recent-requests")
    public ResponseEntity<List<RecentRequestDto>> getRecentRequests(
            @Parameter(description = "Number of recent requests to return", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(dashboardService.getRecentRequests(size));
    }
}
