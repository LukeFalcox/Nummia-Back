package com.devfalco.nummia.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assistant_messages")
@Getter
@Setter
@NoArgsConstructor
public class AssistantMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageRole role;

    @Column(nullable = false, length = 8000)
    private String content;

    /** Origem da resposta: REMOTE (serviço de IA) ou LOCAL (regras internas). */
    @Column(length = 20)
    private String source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
