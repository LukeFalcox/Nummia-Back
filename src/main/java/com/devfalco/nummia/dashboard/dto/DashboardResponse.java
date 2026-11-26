package com.devfalco.nummia.dashboard.dto;

import com.devfalco.nummia.account.dto.AccountResponse;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.MonthlyTotal;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.goal.dto.GoalResponse;
import com.devfalco.nummia.transaction.dto.TransactionResponse;

import java.util.List;

public record DashboardResponse(
        FinancialSummary summary,
        List<MonthlyTotal> monthlyEvolution,
        List<AccountResponse> accounts,
        List<BudgetResponse> budgets,
        List<GoalResponse> goals,
        List<TransactionResponse> recentTransactions,
        long unreadNotifications
) {
}
