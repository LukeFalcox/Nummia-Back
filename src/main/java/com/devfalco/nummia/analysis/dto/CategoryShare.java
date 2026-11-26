package com.devfalco.nummia.analysis.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryShare(UUID categoryId, String name, String color, String icon, BigDecimal total,
                            BigDecimal percentage) {
}
