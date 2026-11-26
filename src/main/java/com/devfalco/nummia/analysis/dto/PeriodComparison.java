package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record PeriodComparison(
        YearMonth currentMonth,
        YearMonth previousMonth,
        BigDecimal currentIncome,
        BigDecimal previousIncome,
        BigDecimal currentExpense,
        BigDecimal previousExpense,
        BigDecimal expenseChangePercentage,
        List<CategoryComparison> categories
) {
}
