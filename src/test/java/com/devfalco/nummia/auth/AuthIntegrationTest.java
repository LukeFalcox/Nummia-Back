package com.devfalco.nummia.auth;

import com.devfalco.nummia.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTest {

    private static final String USER = """
            {"name":"Maria","email":"maria@nummia.test","password":"senha1234"}""";

    @Test
    void registerLoginRefreshAndLogout() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(USER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("maria@nummia.test"));

        // e-mail duplicado (case-insensitive)
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(USER.replace("maria@", "MARIA@")))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@nummia.test","password":"errada123"}"""))
                .andExpect(status().isUnauthorized());

        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"maria@nummia.test","password":"senha1234"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String access = JsonPath.read(login, "$.accessToken");
        String refresh = JsonPath.read(login, "$.refreshToken");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria"));

        String refreshed = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String newRefresh = JsonPath.read(refreshed, "$.refreshToken");

        // reuso do token antigo é rejeitado e revoga todas as sessões
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + newRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWeakPasswordAndInvalidEmail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"X","email":"invalido","password":"123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/accounts").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}
