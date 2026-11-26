package com.devfalco.nummia.transaction;

import java.math.BigDecimal;
import java.util.UUID;

public record CategoryTotal(UUID categoryId, String name, String color, String icon, BigDecimal total) {
}
