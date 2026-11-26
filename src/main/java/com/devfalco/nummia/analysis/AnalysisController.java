package com.devfalco.nummia.analysis;

import com.devfalco.nummia.analysis.dto.Anomaly;
import com.devfalco.nummia.analysis.dto.BudgetSuggestion;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.MonthlyTotal;
import com.devfalco.nummia.analysis.dto.PeriodComparison;
import com.devfalco.nummia.analysis.dto.Recommendation;
import com.devfalco.nummia.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Análises", description = "Agente de Análise e Planejamento: comparações, anomalias e recomendações")
@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService service;
    private final CurrentUser currentUser;

    @Operation(summary = "Resumo financeiro do mês (receitas, despesas, poupança e distribuição por categoria)")
    @GetMapping("/summary")
    public FinancialSummary summary(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.summary(currentUser.id(), month);
    }

    @Operation(summary = "Evolução mensal de receitas e despesas")
    @GetMapping("/evolution")
    public List<MonthlyTotal> evolution(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth until,
                                        @RequestParam(defaultValue = "6") int months) {
        return service.evolution(currentUser.id(), until, Math.clamp(months, 1, 24));
    }

    @Operation(summary = "Compara gastos por categoria com o mês anterior")
    @GetMapping("/comparison")
    public PeriodComparison comparison(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.compare(currentUser.id(), month);
    }

    @Operation(summary = "Movimentações fora do padrão no mês")
    @GetMapping("/anomalies")
    public List<Anomaly> anomalies(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month,
                                   @RequestParam(defaultValue = "3") int lookbackMonths) {
        return service.anomalies(currentUser.id(), month, Math.clamp(lookbackMonths, 1, 12));
    }

    @Operation(summary = "Recomendações e oportunidades de economia")
    @GetMapping("/recommendations")
    public List<Recommendation> recommendations(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.recommendations(currentUser.id(), month);
    }

    @Operation(summary = "Sugestões de limite mensal por categoria (Agente de Planejamento)")
    @GetMapping("/budget-suggestions")
    public List<BudgetSuggestion> budgetSuggestions(@RequestParam(defaultValue = "3") int lookbackMonths) {
        return service.budgetSuggestions(currentUser.id(), Math.clamp(lookbackMonths, 1, 12));
    }
}
