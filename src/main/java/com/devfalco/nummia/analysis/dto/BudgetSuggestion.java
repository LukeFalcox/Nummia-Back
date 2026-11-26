package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetSuggestion(UUID categoryId, String categoryName, BigDecimal averageMonthlySpending,
                               BigDecimal suggestedLimit, BigDecimal currentLimit) {
}
