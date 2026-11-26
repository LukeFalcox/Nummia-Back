package com.devfalco.nummia.transaction.dto;

import com.devfalco.nummia.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionFilter(
        LocalDate from,
        LocalDate to,
        TransactionType type,
        UUID accountId,
        UUID categoryId,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Boolean recurring,
        String search
) {
}
