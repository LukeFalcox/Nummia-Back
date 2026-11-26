package com.devfalco.nummia.assistant.ai;

import com.devfalco.nummia.config.NummiaProperties;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Encaminha a conversa para o serviço de IA configurado em {@code nummia.ai.webhook-url}
 * (fluxo n8n com AI Agent, serviço Python/LangGraph etc.).
 *
 * <p>Contrato esperado da resposta (JSON): {@code {"reply": "texto", "suggestions": ["..."]}}.
 * Também é aceito {@code {"output": "texto"}} (formato padrão do AI Agent do n8n).
 */
@Component
public class WebhookAiClient implements AiClient {

    private final NummiaProperties.Ai config;
    private final RestClient restClient;

    public WebhookAiClient(NummiaProperties properties, RestClient.Builder builder) {
        this.config = properties.ai();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(config.timeout());
        factory.setReadTimeout(config.timeout());
        this.restClient = builder.clone().requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return config.enabled();
    }

    @Override
    @SuppressWarnings("unchecked")
    public AiReply chat(AiRequest request) {
        var spec = restClient.post()
                .uri(config.webhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON);
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            spec = spec.header("Authorization", "Bearer " + config.apiKey());
        }
        Map<String, Object> body = spec.body(request).retrieve().body(Map.class);
        if (body == null) {
            throw new IllegalStateException("Resposta vazia do serviço de IA.");
        }
        Object reply = body.getOrDefault("reply", body.get("output"));
        if (!(reply instanceof String text) || text.isBlank()) {
            throw new IllegalStateException("Resposta do serviço de IA sem o campo 'reply'.");
        }
        List<String> suggestions = body.get("suggestions") instanceof List<?> list
                ? list.stream().map(String::valueOf).toList()
                : List.of();
        return new AiReply(text, "REMOTE", suggestions);
    }
}
