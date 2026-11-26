package com.devfalco.nummia.automation;

import com.devfalco.nummia.config.NummiaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Envia eventos da plataforma para o webhook de automações (ex.: n8n), que pode
 * disparar e-mails, push, WhatsApp etc. O corpo é assinado com HMAC-SHA256 no
 * cabeçalho {@code X-Nummia-Signature} quando um segredo é configurado.
 */
@Slf4j
@Component
public class AutomationEventPublisher {

    private final NummiaProperties.Automation config;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public AutomationEventPublisher(NummiaProperties properties, JsonMapper jsonMapper, RestClient.Builder builder) {
        this.config = properties.automation();
        this.jsonMapper = jsonMapper;
        this.restClient = builder.build();
    }

    @Async
    public void publish(String event, UUID userId, Map<String, ?> data) {
        if (!config.enabled()) {
            return;
        }
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", event);
            payload.put("userId", userId.toString());
            payload.put("occurredAt", Instant.now().toString());
            payload.put("data", data);
            String body = jsonMapper.writeValueAsString(payload);
            var request = restClient.post()
                    .uri(config.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Nummia-Event", event);
            if (config.secret() != null && !config.secret().isBlank()) {
                request = request.header("X-Nummia-Signature", "sha256=" + hmac(body, config.secret()));
            }
            request.body(body).retrieve().toBodilessEntity();
        } catch (Exception e) {
            log.warn("Falha ao enviar evento {} para automações: {}", event, e.getMessage());
        }
    }

    static String hmac(String body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
    }
}
