package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyTotal(YearMonth month, BigDecimal income, BigDecimal expense, BigDecimal result) {
}
