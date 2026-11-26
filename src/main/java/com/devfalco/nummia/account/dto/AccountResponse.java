package com.devfalco.nummia.account.dto;

import com.devfalco.nummia.account.AccountType;
import com.devfalco.nummia.account.FinancialAccount;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
        UUID id,
        String name,
        AccountType type,
        String institution,
        BigDecimal initialBalance,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        String color,
        boolean archived
) {
    public static AccountResponse from(FinancialAccount a, BigDecimal currentBalance) {
        return new AccountResponse(a.getId(), a.getName(), a.getType(), a.getInstitution(), a.getInitialBalance(),
                currentBalance, a.getCreditLimit(), a.getColor(), a.isArchived());
    }
}
