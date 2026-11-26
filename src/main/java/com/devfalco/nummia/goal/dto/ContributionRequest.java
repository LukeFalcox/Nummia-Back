package com.devfalco.nummia.goal.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Valor positivo para aporte e negativo para resgate. */
public record ContributionRequest(
        @NotNull @Digits(integer = 13, fraction = 2) BigDecimal amount,
        LocalDate date,
        @Size(max = 200) String note
) {
}
