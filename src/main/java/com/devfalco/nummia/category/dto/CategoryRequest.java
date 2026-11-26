package com.devfalco.nummia.category.dto;

import com.devfalco.nummia.category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 60) String name,
        @NotNull CategoryType type,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Cor deve estar no formato #RRGGBB") String color,
        @Size(max = 40) String icon,
        Boolean archived
) {
}
