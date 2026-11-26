package com.devfalco.nummia.goal.dto;

import com.devfalco.nummia.goal.GoalStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal targetAmount,
        @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal initialAmount,
        LocalDate deadline,
        GoalStatus status,
        @Size(max = 40) String icon,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato #RRGGBB") String color
) {
}
