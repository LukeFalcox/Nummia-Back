package com.devfalco.nummia.assistant.ai;

import com.devfalco.nummia.analysis.dto.CategoryComparison;
import com.devfalco.nummia.analysis.dto.CategoryShare;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.Recommendation;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.goal.dto.GoalResponse;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Assistente local baseado em regras. Usado quando não há serviço de IA configurado
 * ou quando ele está indisponível, garantindo respostas úteis a partir dos dados reais.
 */
@Component
public class LocalAiClient implements AiClient {

    private static final List<String> DEFAULT_SUGGESTIONS = List.of(
            "Qual é o meu saldo?",
            "Quais foram meus maiores gastos este mês?",
            "Como estão meus orçamentos?",
            "Como posso economizar?");

    @Override
    public AiReply chat(AiRequest request) {
        String q = normalize(request.message());
        FinancialContext ctx = request.context();
        String reply;
        if (containsAny(q, "saldo", "quanto tenho", "conta")) {
            reply = balance(ctx);
        } else if (containsAny(q, "compar", "mes passado", "mes anterior", "aument")) {
            reply = comparison(ctx);
        } else if (containsAny(q, "orcamento", "limite")) {
            reply = budgets(ctx);
        } else if (containsAny(q, "meta", "objetivo")) {
            reply = goals(ctx);
        } else if (containsAny(q, "economiz", "dica", "recomend", "sugest", "melhorar")) {
            reply = recommendations(ctx);
        } else if (containsAny(q, "receita", "ganhei", "renda", "recebi", "salario")) {
            reply = incomes(ctx);
        } else if (containsAny(q, "gast", "despesa", "paguei", "onde")) {
            reply = expenses(ctx);
        } else {
            reply = overview(ctx);
        }
        return new AiReply(reply, "LOCAL", DEFAULT_SUGGESTIONS);
    }

    private String balance(FinancialContext ctx) {
        String perAccount = ctx.accounts().stream()
                .map(a -> "• %s: R$ %s".formatted(a.name(), a.currentBalance()))
                .collect(Collectors.joining("\n"));
        return "Seu saldo consolidado é de R$ %s.%s".formatted(ctx.summary().totalBalance(),
                perAccount.isEmpty() ? "" : "\n\n" + perAccount);
    }

    private String expenses(FinancialContext ctx) {
        FinancialSummary s = ctx.summary();
        if (s.expense().signum() == 0) {
            return "Você ainda não registrou despesas em " + s.month() + ".";
        }
        return "Em %s você gastou R$ %s. Principais categorias:\n%s".formatted(s.month(), s.expense(),
                top(s.expensesByCategory()));
    }

    private String incomes(FinancialContext ctx) {
        FinancialSummary s = ctx.summary();
        if (s.income().signum() == 0) {
            return "Nenhuma receita registrada em " + s.month() + ".";
        }
        return "Em %s você recebeu R$ %s:\n%s".formatted(s.month(), s.income(), top(s.incomesByCategory()));
    }

    private String comparison(FinancialContext ctx) {
        var c = ctx.comparison();
        StringBuilder sb = new StringBuilder("Despesas: R$ %s em %s contra R$ %s em %s"
                .formatted(c.currentExpense(), c.currentMonth(), c.previousExpense(), c.previousMonth()));
        if (c.expenseChangePercentage() != null) {
            sb.append(" (").append(c.expenseChangePercentage().signum() >= 0 ? "+" : "")
                    .append(c.expenseChangePercentage()).append("%)");
        }
        sb.append('.');
        List<CategoryComparison> rising = c.categories().stream()
                .filter(x -> x.trend() == CategoryComparison.Trend.UP).limit(3).toList();
        if (!rising.isEmpty()) {
            sb.append("\n\nCategorias que mais aumentaram:\n");
            rising.forEach(x -> sb.append("• %s: +R$ %s\n".formatted(x.name(), x.difference())));
        }
        return sb.toString().trim();
    }

    private String budgets(FinancialContext ctx) {
        if (ctx.budgets().isEmpty()) {
            return "Você ainda não definiu orçamentos para este mês. Que tal criar limites para suas principais categorias?";
        }
        return "Situação dos seus orçamentos:\n" + ctx.budgets().stream()
                .map(LocalAiClient::budgetLine).collect(Collectors.joining("\n"));
    }

    private String goals(FinancialContext ctx) {
        if (ctx.goals().isEmpty()) {
            return "Você não possui metas ativas. Criar uma meta ajuda a direcionar sua economia mensal!";
        }
        return "Suas metas ativas:\n" + ctx.goals().stream().map(LocalAiClient::goalLine).collect(Collectors.joining("\n"));
    }

    private String recommendations(FinancialContext ctx) {
        if (ctx.recommendations().isEmpty()) {
            return "Suas finanças parecem equilibradas neste mês. Continue registrando suas movimentações!";
        }
        return "Algumas observações sobre suas finanças:\n" + ctx.recommendations().stream()
                .limit(5).map(Recommendation::message).map(m -> "• " + m).collect(Collectors.joining("\n"));
    }

    private String overview(FinancialContext ctx) {
        FinancialSummary s = ctx.summary();
        return ("Resumo de %s: receitas de R$ %s, despesas de R$ %s e resultado de R$ %s. Saldo consolidado: R$ %s.\n\n"
                + "Posso ajudar com saldo, gastos, orçamentos, metas, comparações entre meses e dicas de economia.")
                .formatted(s.month(), s.income(), s.expense(), s.result(), s.totalBalance());
    }

    private static String top(List<CategoryShare> shares) {
        return shares.stream().limit(3)
                .map(c -> "• %s: R$ %s (%s%%)".formatted(c.name(), c.total(), c.percentage()))
                .collect(Collectors.joining("\n"));
    }

    private static String goalLine(GoalResponse g) {
        String line = "• %s: R$ %s de R$ %s (%s%%)".formatted(g.name(), g.currentAmount(), g.targetAmount(), g.progressPercentage());
        return g.monthlyAmountNeeded() != null ? line + " — guarde R$ %s/mês para cumprir o prazo".formatted(g.monthlyAmountNeeded()) : line;
    }

    private static String budgetLine(BudgetResponse b) {
        String status = switch (b.status()) {
            case OK -> "dentro do limite";
            case WARNING -> "atenção";
            case EXCEEDED -> "limite ultrapassado";
        };
        return "• %s: R$ %s de R$ %s (%s%%) — %s".formatted(b.categoryName(), b.spent(), b.limitAmount(),
                b.usedPercentage(), status);
    }

    private static boolean containsAny(String text, String... terms) {
        for (String t : terms) {
            if (text.contains(t)) return true;
        }
        return false;
    }

    private static String normalize(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
