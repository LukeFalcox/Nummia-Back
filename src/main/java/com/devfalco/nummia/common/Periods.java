package com.devfalco.nummia.common;

import java.time.LocalDate;
import java.time.YearMonth;

public final class Periods {

    private Periods() {
    }

    public static YearMonth orCurrent(YearMonth month) {
        return month != null ? month : YearMonth.now();
    }

    public static LocalDate start(YearMonth month) {
        return month.atDay(1);
    }

    public static LocalDate end(YearMonth month) {
        return month.atEndOfMonth();
    }
}
