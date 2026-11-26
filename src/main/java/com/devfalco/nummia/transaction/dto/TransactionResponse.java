package com.devfalco.nummia.transaction.dto;

import com.devfalco.nummia.transaction.Transaction;
import com.devfalco.nummia.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType type,
        BigDecimal amount,
        String description,
        LocalDate date,
        Ref account,
        Ref destinationAccount,
        CategoryRef category,
        String notes,
        boolean recurring,
        Instant createdAt
) {
    public record Ref(UUID id, String name) {
    }

    public record CategoryRef(UUID id, String name, String color, String icon) {
    }

    public static TransactionResponse from(Transaction t) {
        var dest = t.getDestinationAccount();
        var cat = t.getCategory();
        return new TransactionResponse(
                t.getId(), t.getType(), t.getAmount(), t.getDescription(), t.getDate(),
                new Ref(t.getAccount().getId(), t.getAccount().getName()),
                dest == null ? null : new Ref(dest.getId(), dest.getName()),
                cat == null ? null : new CategoryRef(cat.getId(), cat.getName(), cat.getColor(), cat.getIcon()),
                t.getNotes(), t.isRecurring(), t.getCreatedAt());
    }
}
