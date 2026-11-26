package com.devfalco.nummia.dashboard;

import com.devfalco.nummia.account.AccountService;
import com.devfalco.nummia.analysis.AnalysisService;
import com.devfalco.nummia.budget.BudgetService;
import com.devfalco.nummia.common.Periods;
import com.devfalco.nummia.dashboard.dto.DashboardResponse;
import com.devfalco.nummia.goal.GoalService;
import com.devfalco.nummia.goal.GoalStatus;
import com.devfalco.nummia.notification.NotificationService;
import com.devfalco.nummia.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AnalysisService analysis;
    private final AccountService accounts;
    private final BudgetService budgets;
    private final GoalService goals;
    private final TransactionService transactions;
    private final NotificationService notifications;

    @Transactional(readOnly = true)
    public DashboardResponse build(UUID userId, YearMonth month) {
        YearMonth m = Periods.orCurrent(month);
        return new DashboardResponse(
                analysis.summary(userId, m),
                analysis.evolution(userId, m, 6),
                accounts.list(userId, false),
                budgets.list(userId, m),
                goals.list(userId, GoalStatus.ACTIVE),
                transactions.recent(userId, 5),
                notifications.unreadCount(userId));
    }
}
