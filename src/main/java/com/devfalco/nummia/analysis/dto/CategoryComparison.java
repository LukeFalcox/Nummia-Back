package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryComparison(
        UUID categoryId,
        String name,
        String color,
        BigDecimal current,
        BigDecimal previous,
        BigDecimal difference,
        /* Variação percentual; nula quando não houve gasto no período anterior. */
        BigDecimal changePercentage,
        Trend trend
) {
    public enum Trend { UP, DOWN, STABLE, NEW }
}
