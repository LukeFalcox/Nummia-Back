package com.devfalco.nummia.analysis;

import com.devfalco.nummia.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalysisIntegrationTest extends IntegrationTest {

    @Test
    void detectsAnomaliesComparesPeriodsAndSuggestsBudgets() throws Exception {
        String token = registerUser();
        String account = createAccount(token, "Conta", "10000");
        String food = categoryId(token, "EXPENSE", "Alimentação");
        String leisure = categoryId(token, "EXPENSE", "Lazer");
        YearMonth current = YearMonth.now();

        // histórico: gastos regulares de ~100 em alimentação nos 3 meses anteriores
        for (int m = 1; m <= 3; m++) {
            YearMonth past = current.minusMonths(m);
            createTransaction(token, "EXPENSE", "100", past.atDay(5).toString(), account, food);
            createTransaction(token, "EXPENSE", "110", past.atDay(15).toString(), account, food);
            createTransaction(token, "EXPENSE", "90", past.atDay(25).toString(), account, food);
        }
        createTransaction(token, "EXPENSE", "50", current.minusMonths(1).atDay(10).toString(), account, leisure);

        // mês atual: gasto muito acima do padrão e lazer em alta
        String today = LocalDate.now().toString();
        createTransaction(token, "EXPENSE", "900", today, account, food);
        createTransaction(token, "EXPENSE", "300", today, account, leisure);

        getJson(token, "/api/analysis/anomalies")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].amount").value(900.00))
                .andExpect(jsonPath("$[0].categoryAverage").value(100.00));

        getJson(token, "/api/analysis/comparison")
                .andExpect(jsonPath("$.currentExpense").value(1200.00))
                .andExpect(jsonPath("$.previousExpense").value(350.00))
                .andExpect(jsonPath("$.categories[0].name").value("Alimentação"))
                .andExpect(jsonPath("$.categories[0].trend").value("UP"));

        getJson(token, "/api/analysis/recommendations")
                .andExpect(jsonPath("$[?(@.type == 'RISING_CATEGORY')]").exists())
                .andExpect(jsonPath("$[?(@.type == 'UNUSUAL_TRANSACTION')]").exists());

        getJson(token, "/api/analysis/budget-suggestions")
                .andExpect(jsonPath("$[?(@.categoryName == 'Alimentação')].averageMonthlySpending").value(300.00))
                .andExpect(jsonPath("$[?(@.categoryName == 'Alimentação')].suggestedLimit").value(270.00));

        getJson(token, "/api/analysis/evolution?months=4")
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[3].expense").value(1200.00));
    }
}
