package com.devfalco.nummia.assistant.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatResponse(UUID messageId, String reply, String source, List<String> suggestions, Instant createdAt) {
}
