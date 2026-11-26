package com.devfalco.nummia.account;

import com.devfalco.nummia.account.dto.AccountRequest;
import com.devfalco.nummia.account.dto.AccountResponse;
import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.exception.ConflictException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository repository;
    private final TransactionRepository transactions;

    @Transactional(readOnly = true)
    public List<AccountResponse> list(UUID userId, boolean includeArchived) {
        List<FinancialAccount> accounts = includeArchived
                ? repository.findByUserIdOrderByNameAsc(userId)
                : repository.findByUserIdAndArchivedFalseOrderByNameAsc(userId);
        Map<UUID, BigDecimal> movements = movementsByAccount(userId, LocalDate.now());
        return accounts.stream()
                .map(a -> AccountResponse.from(a, balance(a, movements)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse find(UUID userId, UUID id) {
        FinancialAccount a = get(userId, id);
        return AccountResponse.from(a, balance(a, movementsByAccount(userId, LocalDate.now())));
    }

    /** Saldo consolidado das contas ativas na data informada. */
    @Transactional(readOnly = true)
    public BigDecimal totalBalance(UUID userId, LocalDate until) {
        Map<UUID, BigDecimal> movements = movementsByAccount(userId, until);
        return Money.of(repository.findByUserIdAndArchivedFalseOrderByNameAsc(userId).stream()
                .map(a -> balance(a, movements))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Transactional
    public AccountResponse create(UUID userId, AccountRequest req) {
        String name = req.name().trim();
        if (repository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("Já existe uma conta com este nome.");
        }
        FinancialAccount a = new FinancialAccount();
        a.setUserId(userId);
        apply(a, req, name);
        repository.save(a);
        return AccountResponse.from(a, Money.of(a.getInitialBalance()));
    }

    @Transactional
    public AccountResponse update(UUID userId, UUID id, AccountRequest req) {
        FinancialAccount a = get(userId, id);
        String name = req.name().trim();
        if (repository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, name, id)) {
            throw new ConflictException("Já existe uma conta com este nome.");
        }
        apply(a, req, name);
        return AccountResponse.from(a, balance(a, movementsByAccount(userId, LocalDate.now())));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        FinancialAccount a = get(userId, id);
        if (transactions.existsByAccountIdOrDestinationAccountId(id, id)) {
            throw new ConflictException("Conta possui movimentações. Arquive-a em vez de excluir.");
        }
        repository.delete(a);
    }

    public FinancialAccount get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Conta"));
    }

    private Map<UUID, BigDecimal> movementsByAccount(UUID userId, LocalDate until) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (Object[] row : transactions.signedTotalsByAccount(userId, until)) {
            result.merge((UUID) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        for (Object[] row : transactions.incomingTransfersByAccount(userId, until)) {
            result.merge((UUID) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        return result;
    }

    private static BigDecimal balance(FinancialAccount a, Map<UUID, BigDecimal> movements) {
        return Money.of(a.getInitialBalance().add(movements.getOrDefault(a.getId(), BigDecimal.ZERO)));
    }

    private void apply(FinancialAccount a, AccountRequest req, String name) {
        a.setName(name);
        a.setType(req.type());
        a.setInstitution(req.institution());
        a.setInitialBalance(Money.of(req.initialBalance()));
        a.setCreditLimit(req.creditLimit());
        a.setColor(req.color());
        if (req.archived() != null) a.setArchived(req.archived());
    }
}
