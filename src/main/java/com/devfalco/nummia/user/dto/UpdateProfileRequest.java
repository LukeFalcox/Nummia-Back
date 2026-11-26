package com.devfalco.nummia.user.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^[A-Z]{3}$", message = "Moeda deve seguir o padrão ISO 4217 (ex.: BRL)") String currency,
        @DecimalMin(value = "0.00") @Digits(integer = 13, fraction = 2) BigDecimal monthlyIncome,
        Boolean aiAssistantEnabled,
        Boolean notificationsEnabled
) {
}
