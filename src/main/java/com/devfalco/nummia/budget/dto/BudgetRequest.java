package com.devfalco.nummia.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

public record BudgetRequest(
        @NotNull UUID categoryId,
        /* Mês de referência no formato yyyy-MM. */
        @NotNull YearMonth month,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal limitAmount,
        @Min(1) @Max(100) Integer alertThreshold
) {
}
