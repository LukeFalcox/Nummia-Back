package com.devfalco.nummia.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    private Money() {
    }

    public static BigDecimal of(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    /** Percentual (0-100+) de {@code part} sobre {@code total}, com 2 casas. */
    public static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_EVEN);
    }
}
