package com.devfalco.nummia.budget.dto;

import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record CopyBudgetsRequest(@NotNull YearMonth from, @NotNull YearMonth to) {
}
