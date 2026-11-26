package com.devfalco.nummia.goal.dto;

import com.devfalco.nummia.goal.GoalContribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ContributionResponse(UUID id, BigDecimal amount, LocalDate date, String note) {
    public static ContributionResponse from(GoalContribution c) {
        return new ContributionResponse(c.getId(), c.getAmount(), c.getDate(), c.getNote());
    }
}
