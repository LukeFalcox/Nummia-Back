package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Resumo financeiro calculado pelo Back-End (base para dashboard e assistente de IA). */
public record FinancialSummary(
        YearMonth month,
        BigDecimal totalBalance,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal result,
        /* Percentual da receita que foi poupado no mês. */
        BigDecimal savingsRate,
        List<CategoryShare> expensesByCategory,
        List<CategoryShare> incomesByCategory
) {
}
