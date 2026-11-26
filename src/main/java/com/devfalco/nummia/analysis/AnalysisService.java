package com.devfalco.nummia.analysis;

import com.devfalco.nummia.account.AccountService;
import com.devfalco.nummia.analysis.dto.Anomaly;
import com.devfalco.nummia.analysis.dto.BudgetSuggestion;
import com.devfalco.nummia.analysis.dto.CategoryComparison;
import com.devfalco.nummia.analysis.dto.CategoryShare;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.MonthlyTotal;
import com.devfalco.nummia.analysis.dto.PeriodComparison;
import com.devfalco.nummia.analysis.dto.Recommendation;
import com.devfalco.nummia.budget.BudgetService;
import com.devfalco.nummia.budget.BudgetStatus;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.Periods;
import com.devfalco.nummia.goal.GoalService;
import com.devfalco.nummia.goal.GoalStatus;
import com.devfalco.nummia.goal.dto.GoalResponse;
import com.devfalco.nummia.notification.Severity;
import com.devfalco.nummia.transaction.CategoryTotal;
import com.devfalco.nummia.transaction.Transaction;
import com.devfalco.nummia.transaction.TransactionRepository;
import com.devfalco.nummia.transaction.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agente de Análise e de Planejamento (regras de negócio). Todos os cálculos
 * financeiros ficam aqui; a IA apenas interpreta e apresenta os resultados.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal RISING_THRESHOLD = BigDecimal.valueOf(20);

    private final TransactionRepository transactions;
    private final AccountService accounts;
    private final BudgetService budgets;
    private final GoalService goals;

    public FinancialSummary summary(UUID userId, YearMonth month) {
        YearMonth m = Periods.orCurrent(month);
        LocalDate from = Periods.start(m);
        LocalDate to = Periods.end(m);
        BigDecimal income = Money.of(transactions.sumByType(userId, TransactionType.INCOME, from, to));
        BigDecimal expense = Money.of(transactions.sumByType(userId, TransactionType.EXPENSE, from, to));
        BigDecimal result = income.subtract(expense);
        LocalDate balanceDate = m.equals(YearMonth.now()) ? LocalDate.now() : to;
        return new FinancialSummary(m, accounts.totalBalance(userId, balanceDate), income, expense, result,
                income.signum() > 0 ? Money.percentage(result, income) : Money.zero(),
                shares(transactions.totalsByCategory(userId, TransactionType.EXPENSE, from, to), expense),
                shares(transactions.totalsByCategory(userId, TransactionType.INCOME, from, to), income));
    }

    /** Receitas e despesas dos últimos {@code months} meses (inclusive o mês informado). */
    public List<MonthlyTotal> evolution(UUID userId, YearMonth until, int months) {
        YearMonth end = Periods.orCurrent(until);
        YearMonth start = end.minusMonths(Math.max(1, months) - 1L);
        Map<YearMonth, BigDecimal[]> totals = new LinkedHashMap<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            totals.put(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] row : transactions.monthlyTotals(userId, Periods.start(start), Periods.end(end))) {
            YearMonth m = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            BigDecimal[] values = totals.get(m);
            if (values != null) {
                values[row[2] == TransactionType.INCOME ? 0 : 1] = (BigDecimal) row[3];
            }
        }
        return totals.entrySet().stream()
                .map(e -> new MonthlyTotal(e.getKey(), Money.of(e.getValue()[0]), Money.of(e.getValue()[1]),
                        Money.of(e.getValue()[0].subtract(e.getValue()[1]))))
                .toList();
    }

    /** Compara as despesas por categoria do mês informado com o mês anterior. */
    public PeriodComparison compare(UUID userId, YearMonth month) {
        YearMonth current = Periods.orCurrent(month);
        YearMonth previous = current.minusMonths(1);
        Map<UUID, CategoryTotal> cur = byCategory(userId, current);
        Map<UUID, CategoryTotal> prev = byCategory(userId, previous);

        Map<UUID, CategoryTotal> all = new LinkedHashMap<>(cur);
        prev.forEach(all::putIfAbsent);

        List<CategoryComparison> categories = all.values().stream().map(c -> {
            BigDecimal now = Money.of(cur.containsKey(c.categoryId()) ? cur.get(c.categoryId()).total() : null);
            BigDecimal before = Money.of(prev.containsKey(c.categoryId()) ? prev.get(c.categoryId()).total() : null);
            BigDecimal diff = now.subtract(before);
            BigDecimal change = before.signum() == 0 ? null : Money.percentage(diff, before);
            CategoryComparison.Trend trend = before.signum() == 0 ? CategoryComparison.Trend.NEW
                    : change.abs().compareTo(BigDecimal.valueOf(5)) < 0 ? CategoryComparison.Trend.STABLE
                    : diff.signum() > 0 ? CategoryComparison.Trend.UP : CategoryComparison.Trend.DOWN;
            return new CategoryComparison(c.categoryId(), c.name(), c.color(), now, before, diff, change, trend);
        }).sorted((a, b) -> b.difference().compareTo(a.difference())).toList();

        BigDecimal curExpense = Money.of(transactions.sumByType(userId, TransactionType.EXPENSE, Periods.start(current), Periods.end(current)));
        BigDecimal prevExpense = Money.of(transactions.sumByType(userId, TransactionType.EXPENSE, Periods.start(previous), Periods.end(previous)));
        return new PeriodComparison(current, previous,
                Money.of(transactions.sumByType(userId, TransactionType.INCOME, Periods.start(current), Periods.end(current))),
                Money.of(transactions.sumByType(userId, TransactionType.INCOME, Periods.start(previous), Periods.end(previous))),
                curExpense, prevExpense,
                prevExpense.signum() == 0 ? null : Money.percentage(curExpense.subtract(prevExpense), prevExpense),
                categories);
    }

    /**
     * Identifica despesas fora do padrão: valor acima de média + 2 desvios-padrão
     * (e pelo menos 50% acima da média) da categoria nos meses anteriores.
     */
    public List<Anomaly> anomalies(UUID userId, YearMonth month, int lookbackMonths) {
        YearMonth m = Periods.orCurrent(month);
        YearMonth historyStart = m.minusMonths(Math.max(1, lookbackMonths));
        List<Transaction> history = transactions.findByTypeAndPeriod(userId, TransactionType.EXPENSE,
                Periods.start(historyStart), Periods.end(m.minusMonths(1)));
        Map<UUID, List<BigDecimal>> byCategory = new HashMap<>();
        for (Transaction t : history) {
            byCategory.computeIfAbsent(t.getCategory().getId(), k -> new ArrayList<>()).add(t.getAmount());
        }

        List<Anomaly> result = new ArrayList<>();
        for (Transaction t : transactions.findByTypeAndPeriod(userId, TransactionType.EXPENSE, Periods.start(m), Periods.end(m))) {
            List<BigDecimal> values = byCategory.get(t.getCategory().getId());
            if (values == null || values.size() < 3) {
                continue;
            }
            double mean = values.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0);
            double variance = values.stream().mapToDouble(v -> Math.pow(v.doubleValue() - mean, 2)).sum() / values.size();
            double limit = mean + 2 * Math.sqrt(variance);
            double amount = t.getAmount().doubleValue();
            if (amount > limit && amount > mean * 1.5) {
                BigDecimal avg = BigDecimal.valueOf(mean).setScale(2, RoundingMode.HALF_EVEN);
                result.add(new Anomaly(t.getId(), t.getDescription(), t.getDate(), t.getCategory().getName(),
                        t.getAmount(), avg,
                        "Valor %.1fx maior que a média de R$ %s em %s.".formatted(amount / mean, avg, t.getCategory().getName())));
            }
        }
        return result;
    }

    /** Sugere limites mensais por categoria com base na média dos últimos meses completos. */
    public List<BudgetSuggestion> budgetSuggestions(UUID userId, int lookbackMonths) {
        int months = Math.max(1, lookbackMonths);
        YearMonth end = YearMonth.now().minusMonths(1);
        YearMonth start = end.minusMonths(months - 1L);
        Map<UUID, BigDecimal> currentLimits = budgets.list(userId, YearMonth.now()).stream()
                .collect(Collectors.toMap(BudgetResponse::categoryId, BudgetResponse::limitAmount));
        return transactions.totalsByCategory(userId, TransactionType.EXPENSE, Periods.start(start), Periods.end(end))
                .stream()
                .map(c -> {
                    BigDecimal avg = c.total().divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_EVEN);
                    // Meta de redução de 10%, arredondada para múltiplos de R$ 10.
                    BigDecimal suggested = avg.multiply(BigDecimal.valueOf(0.9))
                            .divide(BigDecimal.TEN, 0, RoundingMode.CEILING).multiply(BigDecimal.TEN);
                    return new BudgetSuggestion(c.categoryId(), c.name(), avg, Money.of(suggested.max(BigDecimal.TEN)),
                            currentLimits.get(c.categoryId()));
                })
                .toList();
    }

    /** Recomendações geradas por regras de negócio a partir dos dados do usuário. */
    public List<Recommendation> recommendations(UUID userId, YearMonth month) {
        YearMonth m = Periods.orCurrent(month);
        List<Recommendation> result = new ArrayList<>();
        FinancialSummary summary = summary(userId, m);

        if (summary.income().signum() > 0) {
            if (summary.result().signum() < 0) {
                result.add(new Recommendation("NEGATIVE_BALANCE", Severity.CRITICAL, "Gastos acima da renda",
                        "Suas despesas superaram suas receitas em R$ %s neste mês. Revise os gastos não essenciais."
                                .formatted(summary.result().negate())));
            } else if (summary.savingsRate().compareTo(BigDecimal.TEN) < 0) {
                result.add(new Recommendation("LOW_SAVINGS", Severity.WARNING, "Taxa de poupança baixa",
                        "Você está poupando %s%% da sua renda. Uma referência saudável é guardar ao menos 10%% a 20%%."
                                .formatted(summary.savingsRate())));
            } else if (summary.savingsRate().compareTo(BigDecimal.valueOf(20)) >= 0) {
                result.add(new Recommendation("GOOD_SAVINGS", Severity.SUCCESS, "Ótima taxa de poupança",
                        "Você poupou %s%% da sua renda neste mês. Considere direcionar o excedente para suas metas."
                                .formatted(summary.savingsRate())));
            }
        }

        for (CategoryComparison c : compare(userId, m).categories()) {
            if (c.trend() == CategoryComparison.Trend.UP && c.changePercentage() != null
                    && c.changePercentage().compareTo(RISING_THRESHOLD) >= 0 && c.difference().compareTo(BigDecimal.valueOf(50)) >= 0) {
                result.add(new Recommendation("RISING_CATEGORY", Severity.WARNING, "Aumento em " + c.name(),
                        "Os gastos com %s subiram %s%% (R$ %s a mais) em relação ao mês anterior."
                                .formatted(c.name(), c.changePercentage(), c.difference())));
            }
        }

        for (BudgetResponse b : budgets.list(userId, m)) {
            if (b.status() == BudgetStatus.EXCEEDED) {
                result.add(new Recommendation("BUDGET_EXCEEDED", Severity.CRITICAL, "Orçamento estourado: " + b.categoryName(),
                        "Você ultrapassou o limite de %s em R$ %s.".formatted(b.categoryName(), b.remaining().negate())));
            } else if (b.status() == BudgetStatus.WARNING) {
                result.add(new Recommendation("BUDGET_WARNING", Severity.WARNING, "Orçamento quase no limite: " + b.categoryName(),
                        "Você já usou %s%% do orçamento de %s.".formatted(b.usedPercentage(), b.categoryName())));
            }
        }

        if (!summary.expensesByCategory().isEmpty()) {
            CategoryShare top = summary.expensesByCategory().getFirst();
            if (top.percentage().compareTo(BigDecimal.valueOf(40)) >= 0) {
                result.add(new Recommendation("CONCENTRATED_SPENDING", Severity.INFO, "Gastos concentrados em " + top.name(),
                        "%s representa %s%% das suas despesas do mês. Avalie se há oportunidades de economia nessa categoria."
                                .formatted(top.name(), top.percentage())));
            }
        }

        for (GoalResponse g : goals.list(userId, GoalStatus.ACTIVE)) {
            if (g.monthlyAmountNeeded() != null && summary.result().signum() > 0
                    && g.monthlyAmountNeeded().compareTo(summary.result()) > 0) {
                result.add(new Recommendation("GOAL_AT_RISK", Severity.WARNING, "Meta em risco: " + g.name(),
                        "Para atingir \"%s\" no prazo você precisa guardar R$ %s por mês, acima da sobra atual de R$ %s."
                                .formatted(g.name(), g.monthlyAmountNeeded(), summary.result())));
            }
        }

        for (Anomaly a : anomalies(userId, m, 3)) {
            result.add(new Recommendation("UNUSUAL_TRANSACTION", Severity.INFO, "Movimentação fora do padrão",
                    "\"%s\" (R$ %s): %s".formatted(a.description(), a.amount(), a.reason())));
        }
        return result;
    }

    private Map<UUID, CategoryTotal> byCategory(UUID userId, YearMonth m) {
        return transactions.totalsByCategory(userId, TransactionType.EXPENSE, Periods.start(m), Periods.end(m)).stream()
                .collect(Collectors.toMap(CategoryTotal::categoryId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    private static List<CategoryShare> shares(List<CategoryTotal> totals, BigDecimal sum) {
        return totals.stream()
                .map(c -> new CategoryShare(c.categoryId(), c.name(), c.color(), c.icon(), Money.of(c.total()),
                        Money.percentage(c.total(), sum)))
                .toList();
    }
}
