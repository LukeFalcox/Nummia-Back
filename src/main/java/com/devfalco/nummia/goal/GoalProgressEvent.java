package com.devfalco.nummia.goal;

import java.util.UUID;

/** Publicado após aportes, para notificações de metas próximas/concluídas. */
public record GoalProgressEvent(UUID userId, UUID goalId) {
}
