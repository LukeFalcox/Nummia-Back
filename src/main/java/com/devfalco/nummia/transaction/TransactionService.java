package com.devfalco.nummia.transaction;

import com.devfalco.nummia.account.AccountService;
import com.devfalco.nummia.account.FinancialAccount;
import com.devfalco.nummia.audit.AuditService;
import com.devfalco.nummia.category.Category;
import com.devfalco.nummia.category.CategoryService;
import com.devfalco.nummia.category.CategoryType;
import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.exception.BusinessException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.common.web.PageResponse;
import com.devfalco.nummia.transaction.dto.TransactionFilter;
import com.devfalco.nummia.transaction.dto.TransactionRequest;
import com.devfalco.nummia.transaction.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repository;
    private final AccountService accounts;
    private final CategoryService categories;
    private final ApplicationEventPublisher events;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(UUID userId, TransactionFilter filter, Pageable pageable) {
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new BusinessException("A data inicial deve ser anterior à data final.");
        }
        Page<Transaction> page = repository.findAll(TransactionSpecifications.matching(userId, filter), pageable);
        return PageResponse.of(page, TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse find(UUID userId, UUID id) {
        return TransactionResponse.from(get(userId, id));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> recent(UUID userId, int limit) {
        return repository.findRecent(userId, PageRequest.of(0, limit)).stream()
                .map(TransactionResponse::from).toList();
    }

    @Transactional
    public TransactionResponse create(UUID userId, TransactionRequest req) {
        Transaction t = new Transaction();
        t.setUserId(userId);
        apply(userId, t, req);
        repository.save(t);
        audit.log(userId, "TRANSACTION_CREATED", "Transaction", t.getId(), t.getType() + " " + t.getAmount());
        publish(t);
        return TransactionResponse.from(t);
    }

    @Transactional
    public TransactionResponse update(UUID userId, UUID id, TransactionRequest req) {
        Transaction t = get(userId, id);
        apply(userId, t, req);
        audit.log(userId, "TRANSACTION_UPDATED", "Transaction", t.getId(), t.getType() + " " + t.getAmount());
        publish(t);
        return TransactionResponse.from(t);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Transaction t = get(userId, id);
        repository.delete(t);
        audit.log(userId, "TRANSACTION_DELETED", "Transaction", id, t.getType() + " " + t.getAmount());
    }

    private Transaction get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Movimentação"));
    }

    private void apply(UUID userId, Transaction t, TransactionRequest req) {
        FinancialAccount account = accounts.get(userId, req.accountId());
        t.setType(req.type());
        t.setAmount(Money.of(req.amount()));
        t.setDescription(req.description().trim());
        t.setDate(req.date());
        t.setNotes(req.notes());
        t.setRecurring(Boolean.TRUE.equals(req.recurring()));
        t.setAccount(account);

        if (req.type() == TransactionType.TRANSFER) {
            if (req.destinationAccountId() == null) {
                throw new BusinessException("Transferências exigem a conta de destino.");
            }
            if (req.destinationAccountId().equals(req.accountId())) {
                throw new BusinessException("A conta de destino deve ser diferente da conta de origem.");
            }
            t.setDestinationAccount(accounts.get(userId, req.destinationAccountId()));
            t.setCategory(null);
        } else {
            if (req.categoryId() == null) {
                throw new BusinessException("Receitas e despesas exigem uma categoria.");
            }
            Category category = categories.get(userId, req.categoryId());
            CategoryType expected = req.type() == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
            if (category.getType() != expected) {
                throw new BusinessException("A categoria informada não corresponde ao tipo da movimentação.");
            }
            t.setCategory(category);
            t.setDestinationAccount(null);
        }
    }

    private void publish(Transaction t) {
        if (t.getType() == TransactionType.EXPENSE && t.getCategory() != null) {
            events.publishEvent(new TransactionChangedEvent(t.getUserId(), t.getCategory().getId(), t.getDate()));
        }
    }
}
