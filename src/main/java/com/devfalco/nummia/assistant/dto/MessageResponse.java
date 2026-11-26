package com.devfalco.nummia.assistant.dto;

import com.devfalco.nummia.assistant.AssistantMessage;
import com.devfalco.nummia.assistant.MessageRole;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(UUID id, MessageRole role, String content, String source, Instant createdAt) {
    public static MessageResponse from(AssistantMessage m) {
        return new MessageResponse(m.getId(), m.getRole(), m.getContent(), m.getSource(), m.getCreatedAt());
    }
}
