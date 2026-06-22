package com.project.ai.controller;

import com.project.ai.dto.HistoryStatsDto;
import com.project.ai.dto.UserHistorySummary;
import com.project.ai.dto.UserMessageDto;
import com.project.ai.service.UserHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 11:46 PM
 */
@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Log4j2
@Tag(name = "User History", description = "Conversation memory management per user")
public class UserHistoryController {

    private final UserHistoryService userHistoryService;

    @Operation(
            summary = "Get all users with message count",
            description = "Returns paginated list of all users who have conversation history with message count and last activity"
    )
    @ApiResponse(responseCode = "200", description = "Users retrieved")
    @GetMapping("/users")
    public ResponseEntity<Page<UserHistorySummary>> getAllUsers(
            @PageableDefault(size = 20, sort = "lastActivity") Pageable pageable) {
        return ResponseEntity.ok(userHistoryService.getAllUsersWithStats(pageable));
    }

    @Operation(
            summary = "Get history for a specific user",
            description = "Returns paginated conversation history for a user ordered by oldest first"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "History retrieved"),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content)
    })
    @GetMapping("/{userId}")
    public ResponseEntity<Page<UserMessageDto>> getUserHistory(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(userHistoryService.getUserHistory(userId, pageable));
    }

    @Operation(
            summary = "Delete specific messages",
            description = "Delete specific conversation messages for a user by message IDs. Only deletes messages that belong to the user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Messages deleted"),
            @ApiResponse(responseCode = "400", description = "None of the provided IDs exist for this user", content = @Content)
    })
    @DeleteMapping("/{userId}/messages")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> deleteMessages(
            @Parameter(description = "User ID", required = true) @PathVariable String userId,
            @RequestBody List<Long> messageIds) {
        userHistoryService.deleteMessages(userId, messageIds);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Clear all history for a user",
            description = "Permanently deletes all conversation memory for a specific user"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "History cleared"),
            @ApiResponse(responseCode = "404", description = "No history found for this user", content = @Content)
    })
    @DeleteMapping("/{userId}")
//    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN', 'ROLE_SUPER_ADMIN')")
    public ResponseEntity<Void> clearUserHistory(
            @Parameter(description = "User ID", required = true) @PathVariable String userId) {
        userHistoryService.clearUserHistory(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get history stats",
            description = "Returns total number of users with conversation history and total message records"
    )
    @ApiResponse(responseCode = "200", description = "Stats retrieved")
    @GetMapping("/stats")
    public ResponseEntity<HistoryStatsDto> getStats() {
        return ResponseEntity.ok(userHistoryService.getStats());
    }
}
