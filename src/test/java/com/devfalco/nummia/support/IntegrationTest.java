package com.devfalco.nummia.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    @Autowired
    protected MockMvc mvc;

    /** Cadastra um usuário novo (e-mail único) e retorna o access token. */
    protected String registerUser() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@nummia.test";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Teste","email":"%s","password":"senha1234"}""".formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    protected ResultActions getJson(String token, String url) throws Exception {
        return mvc.perform(auth(get(url), token));
    }

    protected ResultActions postJson(String token, String url, String json) throws Exception {
        return mvc.perform(auth(post(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putJson(String token, String url, String json) throws Exception {
        return mvc.perform(auth(put(url), token).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteReq(String token, String url) throws Exception {
        return mvc.perform(auth(delete(url), token));
    }

    protected String createAccount(String token, String name, String initialBalance) throws Exception {
        String body = postJson(token, "/api/accounts", """
                {"name":"%s","type":"CHECKING","initialBalance":%s}""".formatted(name, initialBalance))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    protected String categoryId(String token, String type, String name) throws Exception {
        String body = getJson(token, "/api/categories?type=" + type)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<java.util.List<String>>read(body, "$[?(@.name == '%s')].id".formatted(name)).getFirst();
    }

    protected String createTransaction(String token, String type, String amount, String date, String accountId,
                                       String categoryId) throws Exception {
        String body = postJson(token, "/api/transactions", """
                {"type":"%s","amount":%s,"description":"Teste %s","date":"%s","accountId":"%s","categoryId":"%s"}"""
                .formatted(type, amount, type, date, accountId, categoryId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder builder, String token) {
        return token == null ? builder : builder.header("Authorization", "Bearer " + token);
    }
}
