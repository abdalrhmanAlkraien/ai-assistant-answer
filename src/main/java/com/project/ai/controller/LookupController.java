package com.project.ai.controller;

import com.project.ai.dto.lookup.SystemLookupDto;
import com.project.ai.dto.lookup.SystemLookupRequest;
import com.project.ai.service.LookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:18 AM
 */
@RestController
@RequiredArgsConstructor
public class LookupController {

    private final LookupService lookupService;

    // ── Public — active only, filtered by type ────────────────────────────────

    @GetMapping("/api/lookups")
    public ResponseEntity<List<SystemLookupDto>> getLookups(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(lookupService.getLookups(type, activeOnly));
    }

    @GetMapping("/api/lookups/{id}")
    public ResponseEntity<SystemLookupDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(lookupService.getById(id));
    }

    @PostMapping("/api/lookups")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN')")
    public ResponseEntity<SystemLookupDto> create(
            @RequestBody @Valid SystemLookupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lookupService.create(request));
    }

    @PutMapping("/api/lookups/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN')")
    public ResponseEntity<SystemLookupDto> update(
            @PathVariable Long id,
            @RequestBody @Valid SystemLookupRequest request) {
        return ResponseEntity.ok(lookupService.update(id, request));
    }

    @DeleteMapping("/api/lookups/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lookupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/lookups/{id}/toggle")
    @PreAuthorize("hasAnyAuthority('ROLE_MIGFORA_ADMIN')")
    public ResponseEntity<SystemLookupDto> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(lookupService.toggleActive(id));
    }
}
