package com.devfalco.nummia.notification;

import com.devfalco.nummia.budget.BudgetRepository;
import com.devfalco.nummia.budget.BudgetService;
import com.devfalco.nummia.budget.BudgetStatus;
import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.goal.GoalProgressEvent;
import com.devfalco.nummia.goal.GoalRepository;
import com.devfalco.nummia.goal.GoalStatus;
import com.devfalco.nummia.transaction.TransactionChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Agente de Notificações (regras locais): reage a eventos do domínio após o commit
 * e gera alertas de orçamento e de metas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTriggers {

    private final BudgetRepository budgets;
    private final BudgetService budgetService;
    private final GoalRepository goals;
    private final NotificationService notifications;

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTransactionChanged(TransactionChangedEvent event) {
        try {
            YearMonth m = YearMonth.from(event.date());
            budgets.findByUserIdAndCategoryIdAndYearAndMonth(event.userId(), event.categoryId(), m.getYear(), m.getMonthValue())
                    .map(b -> budgetService.toResponse(event.userId(), b))
                    .ifPresent(b -> {
                        if (b.status() == BudgetStatus.EXCEEDED) {
                            notifications.notify(event.userId(), NotificationType.BUDGET_EXCEEDED, Severity.CRITICAL,
                                    "Orçamento estourado: " + b.categoryName(),
                                    "Você gastou R$ %s de R$ %s em %s (%s%%) em %s."
                                            .formatted(b.spent(), b.limitAmount(), b.categoryName(), b.usedPercentage(), m),
                                    "budget-exceeded:" + b.id());
                        } else if (b.status() == BudgetStatus.WARNING) {
                            notifications.notify(event.userId(), NotificationType.BUDGET_WARNING, Severity.WARNING,
                                    "Atenção ao orçamento de " + b.categoryName(),
                                    "Você já utilizou %s%% do limite de %s (restam R$ %s)."
                                            .formatted(b.usedPercentage(), b.categoryName(), b.remaining()),
                                    "budget-warning:" + b.id());
                        }
                    });
        } catch (RuntimeException e) {
            log.warn("Falha ao avaliar alertas de orçamento: {}", e.getMessage());
        }
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onGoalProgress(GoalProgressEvent event) {
        try {
            goals.findByIdAndUserId(event.goalId(), event.userId()).ifPresent(g -> {
                BigDecimal progress = Money.percentage(g.getCurrentAmount(), g.getTargetAmount());
                if (g.getStatus() == GoalStatus.COMPLETED) {
                    notifications.notify(event.userId(), NotificationType.GOAL_COMPLETED, Severity.SUCCESS,
                            "Meta atingida: " + g.getName(),
                            "Parabéns! Você alcançou R$ %s na meta \"%s\".".formatted(g.getCurrentAmount(), g.getName()),
                            "goal-completed:" + g.getId());
                } else if (progress.compareTo(BigDecimal.valueOf(90)) >= 0) {
                    notifications.notify(event.userId(), NotificationType.GOAL_NEAR, Severity.INFO,
                            "Falta pouco para a meta " + g.getName(),
                            "Você já alcançou %s%% da meta \"%s\".".formatted(progress, g.getName()),
                            "goal-near:" + g.getId());
                }
            });
        } catch (RuntimeException e) {
            log.warn("Falha ao avaliar alertas de metas: {}", e.getMessage());
        }
    }
}
