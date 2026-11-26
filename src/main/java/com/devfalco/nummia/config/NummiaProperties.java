package com.devfalco.nummia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;

/**
 * Configurações específicas da NUMMIA ({@code nummia.*}).
 * Segredos devem ser informados via variáveis de ambiente, nunca versionados.
 */
@Validated
@ConfigurationProperties(prefix = "nummia")
public record NummiaProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull Cors cors,
        @Valid @NotNull Ai ai,
        @Valid @NotNull Automation automation
) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "nummia.jwt.secret deve ter no mínimo 32 caracteres") String secret,
            @NotBlank String issuer,
            @NotNull Duration accessTokenTtl,
            @NotNull Duration refreshTokenTtl
    ) {
    }

    public record Cors(List<String> allowedOrigins) {
        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
        }
    }

    /**
     * Serviço externo de IA (n8n, LangGraph, serviço Python...). Quando {@code webhookUrl}
     * estiver vazio, o assistente usa respostas locais baseadas em regras.
     */
    public record Ai(String webhookUrl, String apiKey, Duration timeout, int historySize) {
        public Ai {
            timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
            historySize = historySize <= 0 ? 10 : historySize;
        }

        public boolean enabled() {
            return webhookUrl != null && !webhookUrl.isBlank();
        }
    }

    /** Webhook (ex.: n8n) que recebe eventos da plataforma, como notificações geradas. */
    public record Automation(String webhookUrl, String secret) {
        public boolean enabled() {
            return webhookUrl != null && !webhookUrl.isBlank();
        }
    }
}
