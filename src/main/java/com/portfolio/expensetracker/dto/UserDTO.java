package com.portfolio.expensetracker.dto;

import com.portfolio.expensetracker.entity.User;

import java.util.Set;

public record UserDTO(
        Long id,
        String username,
        String email,
        Set<String> roles
) {
    public static UserDTO fromEntity(User user) {
        return new UserDTO(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRoles().stream()
                        .map(Enum::name)
                        .collect(java.util.stream.Collectors.toSet())
        );
    }
}
