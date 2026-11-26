package com.devfalco.nummia.finance;

import com.devfalco.nummia.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FinanceFlowIntegrationTest extends IntegrationTest {

    private final String today = LocalDate.now().toString();
    private final String month = YearMonth.now().toString();

    @Test
    void balancesConsiderIncomesExpensesAndTransfers() throws Exception {
        String token = registerUser();
        String checking = createAccount(token, "Corrente", "1000.00");
        String savings = createAccount(token, "Poupança", "0");
        createTransaction(token, "INCOME", "3000", today, checking, categoryId(token, "INCOME", "Salário"));
        createTransaction(token, "EXPENSE", "250.50", today, checking, categoryId(token, "EXPENSE", "Transporte"));
        postJson(token, "/api/transactions", """
                {"type":"TRANSFER","amount":500,"description":"Reserva","date":"%s","accountId":"%s","destinationAccountId":"%s"}"""
                .formatted(today, checking, savings)).andExpect(status().isCreated());

        getJson(token, "/api/accounts/" + checking).andExpect(jsonPath("$.currentBalance").value(3249.50));
        getJson(token, "/api/accounts/" + savings).andExpect(jsonPath("$.currentBalance").value(500.00));

        getJson(token, "/api/dashboard")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalBalance").value(3749.50))
                .andExpect(jsonPath("$.summary.income").value(3000.00))
                .andExpect(jsonPath("$.summary.expense").value(250.50))
                .andExpect(jsonPath("$.monthlyEvolution", hasSize(6)))
                .andExpect(jsonPath("$.recentTransactions", hasSize(3)));

        // conta com movimentações não pode ser excluída
        deleteReq(token, "/api/accounts/" + checking).andExpect(status().isConflict());
    }

    @Test
    void validatesBusinessRules() throws Exception {
        String token = registerUser();
        String account = createAccount(token, "Carteira", "0");
        String incomeCategory = categoryId(token, "INCOME", "Salário");

        // despesa com categoria de receita
        postJson(token, "/api/transactions", """
                {"type":"EXPENSE","amount":10,"description":"x","date":"%s","accountId":"%s","categoryId":"%s"}"""
                .formatted(today, account, incomeCategory))
                .andExpect(status().isUnprocessableContent());

        // transferência para a mesma conta
        postJson(token, "/api/transactions", """
                {"type":"TRANSFER","amount":10,"description":"x","date":"%s","accountId":"%s","destinationAccountId":"%s"}"""
                .formatted(today, account, account))
                .andExpect(status().isUnprocessableContent());

        // orçamento para categoria de receita
        postJson(token, "/api/budgets", """
                {"categoryId":"%s","month":"%s","limitAmount":100}""".formatted(incomeCategory, month))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void usersCannotAccessEachOthersData() throws Exception {
        String alice = registerUser();
        String bob = registerUser();
        String aliceAccount = createAccount(alice, "Conta da Alice", "100");
        String txId = createTransaction(alice, "EXPENSE", "10", today, aliceAccount, categoryId(alice, "EXPENSE", "Lazer"));

        getJson(bob, "/api/accounts/" + aliceAccount).andExpect(status().isNotFound());
        getJson(bob, "/api/transactions/" + txId).andExpect(status().isNotFound());
        deleteReq(bob, "/api/transactions/" + txId).andExpect(status().isNotFound());
        getJson(bob, "/api/transactions").andExpect(jsonPath("$.totalElements").value(0));

        // Bob não pode lançar na conta da Alice
        postJson(bob, "/api/transactions", """
                {"type":"EXPENSE","amount":10,"description":"x","date":"%s","accountId":"%s","categoryId":"%s"}"""
                .formatted(today, aliceAccount, categoryId(bob, "EXPENSE", "Lazer")))
                .andExpect(status().isNotFound());
    }

    @Test
    void budgetAlertsGenerateNotifications() throws Exception {
        String token = registerUser();
        String account = createAccount(token, "Conta", "0");
        String food = categoryId(token, "EXPENSE", "Alimentação");
        postJson(token, "/api/budgets", """
                {"categoryId":"%s","month":"%s","limitAmount":200}""".formatted(food, month))
                .andExpect(status().isCreated());

        createTransaction(token, "EXPENSE", "170", today, account, food);
        getJson(token, "/api/budgets?month=" + month)
                .andExpect(jsonPath("$[0].status").value("WARNING"))
                .andExpect(jsonPath("$[0].usedPercentage").value(85.00));

        createTransaction(token, "EXPENSE", "50", today, account, food);
        getJson(token, "/api/budgets?month=" + month).andExpect(jsonPath("$[0].status").value("EXCEEDED"));

        getJson(token, "/api/notifications")
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.type == 'BUDGET_EXCEEDED')]").exists())
                .andExpect(jsonPath("$.content[?(@.type == 'BUDGET_WARNING')]").exists());
        getJson(token, "/api/notifications/unread-count").andExpect(jsonPath("$.count").value(2));

        // novas despesas não duplicam o alerta
        createTransaction(token, "EXPENSE", "5", today, account, food);
        getJson(token, "/api/notifications").andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void goalsTrackContributionsAndCompletion() throws Exception {
        String token = registerUser();
        String body = postJson(token, "/api/goals", """
                {"name":"Reserva de emergência","targetAmount":1000,"initialAmount":200,"deadline":"%s"}"""
                .formatted(LocalDate.now().plusMonths(8)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.progressPercentage").value(20.00))
                .andExpect(jsonPath("$.monthlyAmountNeeded").value(100.00))
                .andReturn().getResponse().getContentAsString();
        String goalId = JsonPath.read(body, "$.id");

        postJson(token, "/api/goals/" + goalId + "/contributions", "{\"amount\":-500}")
                .andExpect(status().isUnprocessableContent());
        postJson(token, "/api/goals/" + goalId + "/contributions", "{\"amount\":800}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        getJson(token, "/api/goals/" + goalId + "/contributions").andExpect(jsonPath("$", hasSize(2)));
        getJson(token, "/api/notifications").andExpect(jsonPath("$.content[0].type").value("GOAL_COMPLETED"));
    }

    @Test
    void filtersAndPaginatesTransactions() throws Exception {
        String token = registerUser();
        String account = createAccount(token, "Conta", "0");
        String food = categoryId(token, "EXPENSE", "Alimentação");
        for (int i = 1; i <= 5; i++) {
            createTransaction(token, "EXPENSE", String.valueOf(i * 10), today, account, food);
        }
        createTransaction(token, "INCOME", "100", today, account, categoryId(token, "INCOME", "Freelance"));

        getJson(token, "/api/transactions?type=EXPENSE&size=2&page=0")
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        getJson(token, "/api/transactions?minAmount=30&type=EXPENSE").andExpect(jsonPath("$.totalElements").value(3));
        getJson(token, "/api/transactions?search=income").andExpect(jsonPath("$.totalElements").value(1));
    }
}
