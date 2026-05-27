package com.project.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 9:38 AM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BulkUploadResponse {

    private int total;
    private int created;
    private int skipped;
    private int failed;
    private String status;
    private List<String> failedIds;
}
