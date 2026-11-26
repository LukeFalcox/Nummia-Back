package com.devfalco.nummia.budget.dto;

import com.devfalco.nummia.budget.BudgetStatus;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID categoryId,
        String categoryName,
        String categoryColor,
        String categoryIcon,
        YearMonth month,
        BigDecimal limitAmount,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal usedPercentage,
        int alertThreshold,
        BudgetStatus status
) {
}
