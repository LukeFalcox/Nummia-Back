package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Anomaly(UUID transactionId, String description, LocalDate date, String categoryName,
                      BigDecimal amount, BigDecimal categoryAverage, String reason) {
}
