package com.project.ai.service;

import com.project.ai.dto.lookup.SystemLookupDto;
import com.project.ai.dto.lookup.SystemLookupRequest;
import com.project.ai.model.lookup.SystemLookup;
import com.project.ai.repository.SystemLookupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:18 AM
 */
@Service
@RequiredArgsConstructor
public class LookupService {

    private final SystemLookupRepository repository;

    public List<SystemLookupDto> getLookups(String type, boolean activeOnly) {
        List<SystemLookup> results;

        if (type != null && !type.isBlank()) {
            // filter by type
            results = activeOnly
                    ? repository.findByTypeAndActiveTrueOrderBySortOrderAsc(type)
                    : repository.findByTypeOrderBySortOrderAsc(type);
        } else {
            // all types
            results = activeOnly
                    ? repository.findByActiveTrueOrderBySortOrderAsc()
                    : repository.findAllByOrderByTypeAscSortOrderAsc();
        }

        return results.stream().map(this::toDto).toList();
    }

    public SystemLookupDto getById(Long id) {
        return toDto(findOrThrow(id));
    }

    public SystemLookupDto create(SystemLookupRequest request) {
        if (repository.existsByTypeAndCode(
                request.getType().toLowerCase().trim(),
                request.getCode().toLowerCase().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lookup with type '" + request.getType()
                            + "' and code '" + request.getCode() + "' already exists");
        }

        SystemLookup entity = SystemLookup.builder()
                .type(request.getType().toLowerCase().trim())
                .code(request.getCode().toLowerCase().trim())
                .label(request.getLabel().trim())
                .active(request.getActive() != null ? request.getActive() : true)
                .sortOrder(request.getSortOrder())
                .build();

        return toDto(repository.save(entity));
    }

    public SystemLookupDto update(Long id, SystemLookupRequest request) {
        SystemLookup entity = findOrThrow(id);

        if (repository.existsByTypeAndCodeAndIdNot(
                request.getType().toLowerCase().trim(),
                request.getCode().toLowerCase().trim(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Lookup with type '" + request.getType()
                            + "' and code '" + request.getCode() + "' already exists");
        }

        entity.setType(request.getType().toLowerCase().trim());
        entity.setCode(request.getCode().toLowerCase().trim());
        entity.setLabel(request.getLabel().trim());
        entity.setActive(request.getActive() != null ? request.getActive() : entity.getActive());
        entity.setSortOrder(request.getSortOrder());

        return toDto(repository.save(entity));
    }

    public void delete(Long id) {
        repository.delete(findOrThrow(id));
    }

    public SystemLookupDto toggleActive(Long id) {
        SystemLookup entity = findOrThrow(id);
        entity.setActive(!entity.getActive());
        return toDto(repository.save(entity));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private SystemLookup findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Lookup not found: " + id));
    }

    private SystemLookupDto toDto(SystemLookup e) {
        return SystemLookupDto.builder()
                .id(e.getId())
                .type(e.getType())
                .code(e.getCode())
                .label(e.getLabel())
                .active(e.getActive())
                .sortOrder(e.getSortOrder())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
