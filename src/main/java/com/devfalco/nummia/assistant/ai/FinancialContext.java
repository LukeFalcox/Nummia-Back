package com.devfalco.nummia.assistant.ai;

import com.devfalco.nummia.account.dto.AccountResponse;
import com.devfalco.nummia.analysis.dto.Anomaly;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.PeriodComparison;
import com.devfalco.nummia.analysis.dto.Recommendation;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.goal.dto.GoalResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Contexto financeiro enviado à IA. Contém apenas dados agregados e calculados pelo
 * Back-End — sem nome, e-mail ou outros dados pessoais (minimização - LGPD).
 */
public record FinancialContext(
        LocalDate today,
        String currency,
        FinancialSummary summary,
        PeriodComparison comparison,
        List<AccountResponse> accounts,
        List<BudgetResponse> budgets,
        List<GoalResponse> goals,
        List<Anomaly> anomalies,
        List<Recommendation> recommendations
) {
}
