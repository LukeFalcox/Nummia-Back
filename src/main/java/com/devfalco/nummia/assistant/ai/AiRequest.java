package com.devfalco.nummia.assistant.ai;

import java.util.List;
import java.util.UUID;

/**
 * Payload enviado ao serviço de IA.
 *
 * @param sessionId identificador pseudonimizado do usuário (memória de conversa no agente)
 * @param message   pergunta do usuário
 * @param history   mensagens anteriores (mais antigas primeiro)
 * @param context   dados financeiros calculados pelo Back-End
 */
public record AiRequest(UUID sessionId, String message, List<HistoryItem> history, FinancialContext context) {

    public record HistoryItem(String role, String content) {
    }
}
