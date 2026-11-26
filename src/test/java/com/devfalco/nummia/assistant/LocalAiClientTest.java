package com.devfalco.nummia.assistant;

import com.devfalco.nummia.analysis.dto.CategoryShare;
import com.devfalco.nummia.analysis.dto.FinancialSummary;
import com.devfalco.nummia.analysis.dto.PeriodComparison;
import com.devfalco.nummia.assistant.ai.AiRequest;
import com.devfalco.nummia.assistant.ai.FinancialContext;
import com.devfalco.nummia.assistant.ai.LocalAiClient;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalAiClientTest {

    private final LocalAiClient client = new LocalAiClient();

    private FinancialContext context() {
        YearMonth m = YearMonth.of(2026, 11);
        var summary = new FinancialSummary(m, new BigDecimal("1500.00"), new BigDecimal("4000.00"),
                new BigDecimal("1000.00"), new BigDecimal("3000.00"), new BigDecimal("75.00"),
                List.of(new CategoryShare(UUID.randomUUID(), "Moradia", null, null, new BigDecimal("1000.00"), new BigDecimal("100.00"))),
                List.of());
        var comparison = new PeriodComparison(m, m.minusMonths(1), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("25.00"), List.of());
        return new FinancialContext(LocalDate.of(2026, 11, 26), "BRL", summary, comparison,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private String ask(String question) {
        return client.chat(new AiRequest(UUID.randomUUID(), question, List.of(), context())).reply();
    }

    @Test
    void understandsIntentsIgnoringAccents() {
        assertThat(ask("Qual é o meu SALDO?")).contains("1500.00");
        assertThat(ask("onde gastei mais?")).contains("Moradia");
        assertThat(ask("compare com o mês passado")).contains("+25.00%");
        assertThat(ask("como estão os orçamentos")).contains("não definiu orçamentos");
        assertThat(ask("olá")).contains("Resumo de 2026-11");
    }
}
