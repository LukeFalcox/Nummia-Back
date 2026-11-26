package com.devfalco.nummia.user.dto;

import com.devfalco.nummia.user.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        String currency,
        BigDecimal monthlyIncome,
        boolean aiAssistantEnabled,
        boolean notificationsEnabled,
        Instant createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.getCurrency(),
                u.getMonthlyIncome(), u.isAiAssistantEnabled(), u.isNotificationsEnabled(), u.getCreatedAt());
    }
}
