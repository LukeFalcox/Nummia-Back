package com.devfalco.nummia.transaction.dto;

import com.devfalco.nummia.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero")
        @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 160) String description,
        @NotNull LocalDate date,
        @NotNull UUID accountId,
        UUID destinationAccountId,
        UUID categoryId,
        @Size(max = 500) String notes,
        Boolean recurring
) {
}
