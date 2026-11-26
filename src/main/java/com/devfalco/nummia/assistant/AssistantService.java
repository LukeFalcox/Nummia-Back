package com.devfalco.nummia.assistant;

import com.devfalco.nummia.account.AccountService;
import com.devfalco.nummia.analysis.AnalysisService;
import com.devfalco.nummia.assistant.ai.AiReply;
import com.devfalco.nummia.assistant.ai.AiRequest;
import com.devfalco.nummia.assistant.ai.FinancialContext;
import com.devfalco.nummia.assistant.ai.LocalAiClient;
import com.devfalco.nummia.assistant.ai.WebhookAiClient;
import com.devfalco.nummia.assistant.dto.ChatResponse;
import com.devfalco.nummia.assistant.dto.MessageResponse;
import com.devfalco.nummia.budget.BudgetService;
import com.devfalco.nummia.common.exception.BusinessException;
import com.devfalco.nummia.config.NummiaProperties;
import com.devfalco.nummia.goal.GoalService;
import com.devfalco.nummia.goal.GoalStatus;
import com.devfalco.nummia.user.User;
import com.devfalco.nummia.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Agente Financeiro: monta o contexto com dados calculados pelo Back-End e delega a
 * interpretação ao serviço de IA. O assistente é somente leitura — não executa
 * transferências, pagamentos ou alterações financeiras.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantMessageRepository messages;
    private final UserService users;
    private final AnalysisService analysis;
    private final AccountService accounts;
    private final BudgetService budgets;
    private final GoalService goals;
    private final WebhookAiClient remoteClient;
    private final LocalAiClient localClient;
    private final NummiaProperties properties;

    @Transactional
    public ChatResponse chat(UUID userId, String message) {
        User user = users.get(userId);
        if (!user.isAiAssistantEnabled()) {
            throw new BusinessException("O assistente de IA está desativado nas suas configurações.");
        }
        List<AiRequest.HistoryItem> history = history(userId);
        save(userId, MessageRole.USER, message.trim(), null);

        AiRequest request = new AiRequest(userId, message.trim(), history, context(userId, user.getCurrency()));
        AiReply reply = ask(request);

        AssistantMessage saved = save(userId, MessageRole.ASSISTANT, reply.reply(), reply.source());
        return new ChatResponse(saved.getId(), saved.getContent(), reply.source(), reply.suggestions(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> messages(UUID userId, int limit) {
        List<MessageResponse> latest = new ArrayList<>(messages.findLatest(userId, PageRequest.of(0, limit)).stream()
                .map(MessageResponse::from).toList());
        Collections.reverse(latest);
        return latest;
    }

    @Transactional
    public void clear(UUID userId) {
        messages.deleteByUserId(userId);
    }

    @Transactional(readOnly = true)
    public FinancialContext context(UUID userId, String currency) {
        YearMonth month = YearMonth.now();
        return new FinancialContext(
                LocalDate.now(),
                currency,
                analysis.summary(userId, month),
                analysis.compare(userId, month),
                accounts.list(userId, false),
                budgets.list(userId, month),
                goals.list(userId, GoalStatus.ACTIVE),
                analysis.anomalies(userId, month, 3),
                analysis.recommendations(userId, month));
    }

    private AiReply ask(AiRequest request) {
        if (remoteClient.isEnabled()) {
            try {
                return remoteClient.chat(request);
            } catch (RuntimeException e) {
                log.warn("Serviço de IA indisponível, usando respostas locais: {}", e.getMessage());
            }
        }
        return localClient.chat(request);
    }

    private List<AiRequest.HistoryItem> history(UUID userId) {
        List<AiRequest.HistoryItem> items = new ArrayList<>(messages
                .findLatest(userId, PageRequest.of(0, properties.ai().historySize())).stream()
                .map(m -> new AiRequest.HistoryItem(m.getRole().name().toLowerCase(), m.getContent()))
                .toList());
        Collections.reverse(items);
        return items;
    }

    private AssistantMessage save(UUID userId, MessageRole role, String content, String source) {
        AssistantMessage m = new AssistantMessage();
        m.setUserId(userId);
        m.setRole(role);
        m.setContent(content.length() > 8000 ? content.substring(0, 8000) : content);
        m.setSource(source);
        m.setCreatedAt(Instant.now());
        return messages.save(m);
    }
}
