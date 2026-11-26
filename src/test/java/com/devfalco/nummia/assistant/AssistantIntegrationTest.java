package com.devfalco.nummia.assistant;

import com.devfalco.nummia.support.IntegrationTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssistantIntegrationTest extends IntegrationTest {

    @Test
    void answersWithLocalRulesAndKeepsHistory() throws Exception {
        String token = registerUser();
        String account = createAccount(token, "Conta", "1500");
        createTransaction(token, "EXPENSE", "320", LocalDate.now().toString(), account,
                categoryId(token, "EXPENSE", "Moradia"));

        postJson(token, "/api/assistant/chat", "{\"message\":\"Qual é o meu saldo?\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("LOCAL"))
                .andExpect(jsonPath("$.reply", containsString("1180.00")));

        postJson(token, "/api/assistant/chat", "{\"message\":\"Onde eu mais gastei?\"}")
                .andExpect(jsonPath("$.reply", containsString("Moradia")));

        getJson(token, "/api/assistant/messages").andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].role").value("USER"));

        getJson(token, "/api/assistant/context")
                .andExpect(jsonPath("$.summary.expense").value(320.00))
                .andExpect(jsonPath("$.email").doesNotExist());

        deleteReq(token, "/api/assistant/messages").andExpect(status().isNoContent());
        getJson(token, "/api/assistant/messages").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void respectsUserPreference() throws Exception {
        String token = registerUser();
        putJson(token, "/api/users/me", "{\"name\":\"Teste\",\"aiAssistantEnabled\":false}").andExpect(status().isOk());
        postJson(token, "/api/assistant/chat", "{\"message\":\"oi\"}").andExpect(status().isUnprocessableContent());
    }
}
