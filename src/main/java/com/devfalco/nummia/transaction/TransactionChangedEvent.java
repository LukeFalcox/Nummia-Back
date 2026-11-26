package com.devfalco.nummia.transaction;

import java.time.LocalDate;
import java.util.UUID;

/** Publicado quando uma despesa é criada/alterada, para verificação de limites de orçamento. */
public record TransactionChangedEvent(UUID userId, UUID categoryId, LocalDate date) {
}
