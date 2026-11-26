package com.devfalco.nummia.account.dto;

import com.devfalco.nummia.account.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AccountRequest(
        @NotBlank @Size(max = 80) String name,
        @NotNull AccountType type,
        @Size(max = 80) String institution,
        @Digits(integer = 13, fraction = 2) BigDecimal initialBalance,
        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal creditLimit,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato #RRGGBB") String color,
        Boolean archived
) {
}
