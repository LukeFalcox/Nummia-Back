package com.devfalco.nummia.notification;

import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.Periods;
import com.devfalco.nummia.transaction.Transaction;
import com.devfalco.nummia.transaction.TransactionRepository;
import com.devfalco.nummia.transaction.TransactionType;
import com.devfalco.nummia.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/** Rotinas periódicas do Agente de Notificações (resumos mensais e despesas recorrentes). */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private static final int RECURRING_REMINDER_DAYS = 3;

    private final UserRepository users;
    private final TransactionRepository transactions;
    private final NotificationService notifications;

    /** Todo dia 1º às 08h: resumo do mês anterior. */
    @Scheduled(cron = "${nummia.schedules.monthly-summary:0 0 8 1 * *}")
    public void monthlySummaries() {
        YearMonth previous = YearMonth.now().minusMonths(1);
        for (UUID userId : users.findIdsWithNotificationsEnabled()) {
            try {
                sendMonthlySummary(userId, previous);
            } catch (RuntimeException e) {
                log.warn("Falha no resumo mensal do usuário {}: {}", userId, e.getMessage());
            }
        }
    }

    /** Diariamente às 09h: lembra despesas recorrentes que vencem nos próximos dias. */
    @Scheduled(cron = "${nummia.schedules.recurring-reminders:0 0 9 * * *}")
    public void recurringReminders() {
        LocalDate today = LocalDate.now();
        for (UUID userId : users.findIdsWithNotificationsEnabled()) {
            try {
                sendRecurringReminders(userId, today);
            } catch (RuntimeException e) {
                log.warn("Falha nos lembretes recorrentes do usuário {}: {}", userId, e.getMessage());
            }
        }
    }

    @Transactional
    public void sendMonthlySummary(UUID userId, YearMonth month) {
        LocalDate from = Periods.start(month);
        LocalDate to = Periods.end(month);
        BigDecimal income = Money.of(transactions.sumByType(userId, TransactionType.INCOME, from, to));
        BigDecimal expense = Money.of(transactions.sumByType(userId, TransactionType.EXPENSE, from, to));
        if (income.signum() == 0 && expense.signum() == 0) {
            return;
        }
        BigDecimal result = income.subtract(expense);
        String outcome = result.signum() >= 0 ? "economizou R$ " + result : "gastou R$ " + result.negate() + " a mais do que recebeu";
        notifications.notify(userId, NotificationType.MONTHLY_SUMMARY, Severity.INFO,
                "Resumo de " + month,
                "Receitas: R$ %s | Despesas: R$ %s. Neste mês você %s.".formatted(income, expense, outcome),
                "monthly-summary:" + month);
    }

    @Transactional
    public void sendRecurringReminders(UUID userId, LocalDate today) {
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        YearMonth current = YearMonth.from(today);
        for (Transaction t : transactions.findRecurringExpenses(userId, Periods.start(lastMonth), Periods.end(lastMonth))) {
            int day = Math.min(t.getDate().getDayOfMonth(), current.lengthOfMonth());
            LocalDate due = current.atDay(day);
            long daysUntil = due.toEpochDay() - today.toEpochDay();
            if (daysUntil >= 0 && daysUntil <= RECURRING_REMINDER_DAYS) {
                notifications.notify(userId, NotificationType.RECURRING_EXPENSE, Severity.INFO,
                        "Despesa recorrente: " + t.getDescription(),
                        "A despesa \"%s\" (R$ %s) costuma ocorrer por volta de %s.".formatted(
                                t.getDescription(), t.getAmount(), due),
                        "recurring:" + t.getId() + ":" + current);
            }
        }
    }
}
