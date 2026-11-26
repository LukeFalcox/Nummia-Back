package com.devfalco.nummia.assistant;

import com.devfalco.nummia.assistant.ai.FinancialContext;
import com.devfalco.nummia.assistant.dto.ChatRequest;
import com.devfalco.nummia.assistant.dto.ChatResponse;
import com.devfalco.nummia.assistant.dto.MessageResponse;
import com.devfalco.nummia.security.CurrentUser;
import com.devfalco.nummia.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Assistente de IA", description = "Agente financeiro conversacional")
@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService service;
    private final UserService users;
    private final CurrentUser currentUser;

    @Operation(summary = "Envia uma pergunta ao assistente financeiro")
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return service.chat(currentUser.id(), request.message());
    }

    @Operation(summary = "Histórico de conversa (mais antigas primeiro)")
    @GetMapping("/messages")
    public List<MessageResponse> messages(@RequestParam(defaultValue = "50") int limit) {
        return service.messages(currentUser.id(), Math.clamp(limit, 1, 200));
    }

    @Operation(summary = "Apaga o histórico de conversa")
    @DeleteMapping("/messages")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear() {
        service.clear(currentUser.id());
    }

    @Operation(summary = "Contexto financeiro que é compartilhado com a IA (transparência)")
    @GetMapping("/context")
    public FinancialContext context() {
        var userId = currentUser.id();
        return service.context(userId, users.get(userId).getCurrency());
    }
}
