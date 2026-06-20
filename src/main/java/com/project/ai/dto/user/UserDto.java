package com.project.ai.dto.user;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 18/06/2026
 * @Time: 7:45 PM
 */
@Builder
@Getter
public class UserDto {
    private String sub;
    private String username;
    private String email;
    private String name;
    private String familyName;
    private String phoneNumber;
    private String status;
    private List<String> groups;
    private LocalDateTime createdAt;
}
