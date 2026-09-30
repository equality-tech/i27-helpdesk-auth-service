package com.i27.helpdesk.auth.dto;

public record UserWithRoleDTO(
        Long id,
        String email,
        String fullName,
        String status,
        String role
) {
}