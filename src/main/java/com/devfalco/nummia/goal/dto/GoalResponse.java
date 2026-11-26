package com.devfalco.nummia.goal.dto;

import com.devfalco.nummia.goal.GoalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        String name,
        String description,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        BigDecimal progressPercentage,
        LocalDate deadline,
        Long monthsRemaining,
        /* Valor mensal necessário para atingir a meta no prazo. */
        BigDecimal monthlyAmountNeeded,
        GoalStatus status,
        String icon,
        String color
) {
}
