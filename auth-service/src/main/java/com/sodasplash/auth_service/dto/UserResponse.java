package com.sodasplash.auth_service.dto;


import com.sodasplash.auth_service.entity.Role;
import lombok.Builder;

@Builder
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean isActive
) {
}
