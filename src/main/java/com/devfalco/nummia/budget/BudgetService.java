package com.devfalco.nummia.budget;

import com.devfalco.nummia.budget.dto.BudgetRequest;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.category.Category;
import com.devfalco.nummia.category.CategoryService;
import com.devfalco.nummia.category.CategoryType;
import com.devfalco.nummia.common.Money;
import com.devfalco.nummia.common.Periods;
import com.devfalco.nummia.common.exception.BusinessException;
import com.devfalco.nummia.common.exception.ConflictException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.transaction.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository repository;
    private final CategoryService categories;
    private final TransactionRepository transactions;

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(UUID userId, YearMonth month) {
        YearMonth m = Periods.orCurrent(month);
        return repository.findByUserIdAndYearAndMonth(userId, m.getYear(), m.getMonthValue()).stream()
                .map(b -> toResponse(userId, b))
                .sorted(Comparator.comparing(BudgetResponse::usedPercentage).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse find(UUID userId, UUID id) {
        return toResponse(userId, get(userId, id));
    }

    @Transactional
    public BudgetResponse create(UUID userId, BudgetRequest req) {
        Category category = expenseCategory(userId, req.categoryId());
        YearMonth m = req.month();
        if (repository.existsByUserIdAndCategoryIdAndYearAndMonth(userId, category.getId(), m.getYear(), m.getMonthValue())) {
            throw new ConflictException("Já existe um orçamento para esta categoria neste mês.");
        }
        Budget b = new Budget();
        b.setUserId(userId);
        b.setCategory(category);
        b.setYear(m.getYear());
        b.setMonth(m.getMonthValue());
        b.setLimitAmount(Money.of(req.limitAmount()));
        if (req.alertThreshold() != null) b.setAlertThreshold(req.alertThreshold());
        repository.save(b);
        return toResponse(userId, b);
    }

    @Transactional
    public BudgetResponse update(UUID userId, UUID id, BudgetRequest req) {
        Budget b = get(userId, id);
        YearMonth m = req.month();
        boolean keyChanged = !b.getCategory().getId().equals(req.categoryId()) || !b.period().equals(m);
        if (keyChanged && repository.existsByUserIdAndCategoryIdAndYearAndMonth(userId, req.categoryId(), m.getYear(), m.getMonthValue())) {
            throw new ConflictException("Já existe um orçamento para esta categoria neste mês.");
        }
        b.setCategory(expenseCategory(userId, req.categoryId()));
        b.setYear(m.getYear());
        b.setMonth(m.getMonthValue());
        b.setLimitAmount(Money.of(req.limitAmount()));
        if (req.alertThreshold() != null) b.setAlertThreshold(req.alertThreshold());
        return toResponse(userId, b);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        repository.delete(get(userId, id));
    }

    /** Copia os orçamentos de um mês para outro (ignora categorias que já possuem orçamento no destino). */
    @Transactional
    public List<BudgetResponse> copy(UUID userId, YearMonth from, YearMonth to) {
        if (from.equals(to)) {
            throw new BusinessException("Os meses de origem e destino devem ser diferentes.");
        }
        for (Budget source : repository.findByUserIdAndYearAndMonth(userId, from.getYear(), from.getMonthValue())) {
            UUID categoryId = source.getCategory().getId();
            if (repository.existsByUserIdAndCategoryIdAndYearAndMonth(userId, categoryId, to.getYear(), to.getMonthValue())) {
                continue;
            }
            Budget b = new Budget();
            b.setUserId(userId);
            b.setCategory(source.getCategory());
            b.setYear(to.getYear());
            b.setMonth(to.getMonthValue());
            b.setLimitAmount(source.getLimitAmount());
            b.setAlertThreshold(source.getAlertThreshold());
            repository.save(b);
        }
        return list(userId, to);
    }

    public BudgetResponse toResponse(UUID userId, Budget b) {
        YearMonth m = b.period();
        BigDecimal spent = Money.of(transactions.sumExpensesByCategory(
                userId, b.getCategory().getId(), Periods.start(m), Periods.end(m)));
        BigDecimal used = Money.percentage(spent, b.getLimitAmount());
        BudgetStatus status = used.compareTo(BigDecimal.valueOf(100)) > 0 ? BudgetStatus.EXCEEDED
                : used.compareTo(BigDecimal.valueOf(b.getAlertThreshold())) >= 0 ? BudgetStatus.WARNING
                : BudgetStatus.OK;
        Category c = b.getCategory();
        return new BudgetResponse(b.getId(), c.getId(), c.getName(), c.getColor(), c.getIcon(), m,
                b.getLimitAmount(), spent, Money.of(b.getLimitAmount().subtract(spent)), used,
                b.getAlertThreshold(), status);
    }

    private Budget get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Orçamento"));
    }

    private Category expenseCategory(UUID userId, UUID categoryId) {
        Category category = categories.get(userId, categoryId);
        if (category.getType() != CategoryType.EXPENSE) {
            throw new BusinessException("Orçamentos só podem ser definidos para categorias de despesa.");
        }
        return category;
    }
}
