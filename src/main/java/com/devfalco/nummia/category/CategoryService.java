package com.devfalco.nummia.category;

import com.devfalco.nummia.category.dto.CategoryRequest;
import com.devfalco.nummia.category.dto.CategoryResponse;
import com.devfalco.nummia.common.exception.ConflictException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.transaction.TransactionRepository;
import com.devfalco.nummia.budget.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private record Default(String name, CategoryType type, String color, String icon) {
    }

    private static final List<Default> DEFAULTS = List.of(
            new Default("Salário", CategoryType.INCOME, "#16A34A", "briefcase"),
            new Default("Freelance", CategoryType.INCOME, "#22C55E", "laptop"),
            new Default("Investimentos", CategoryType.INCOME, "#0EA5E9", "trending-up"),
            new Default("Outras receitas", CategoryType.INCOME, "#84CC16", "plus-circle"),
            new Default("Moradia", CategoryType.EXPENSE, "#6366F1", "home"),
            new Default("Alimentação", CategoryType.EXPENSE, "#F97316", "utensils"),
            new Default("Transporte", CategoryType.EXPENSE, "#EAB308", "car"),
            new Default("Saúde", CategoryType.EXPENSE, "#EF4444", "heart-pulse"),
            new Default("Educação", CategoryType.EXPENSE, "#8B5CF6", "graduation-cap"),
            new Default("Lazer", CategoryType.EXPENSE, "#EC4899", "party-popper"),
            new Default("Compras", CategoryType.EXPENSE, "#14B8A6", "shopping-bag"),
            new Default("Assinaturas", CategoryType.EXPENSE, "#A855F7", "repeat"),
            new Default("Contas e serviços", CategoryType.EXPENSE, "#64748B", "receipt"),
            new Default("Outras despesas", CategoryType.EXPENSE, "#94A3B8", "circle-ellipsis")
    );

    private final CategoryRepository repository;
    private final TransactionRepository transactions;
    private final BudgetRepository budgets;

    @Transactional
    public void createDefaults(UUID userId) {
        List<Category> categories = DEFAULTS.stream().map(d -> {
            Category c = new Category();
            c.setUserId(userId);
            c.setName(d.name());
            c.setType(d.type());
            c.setColor(d.color());
            c.setIcon(d.icon());
            return c;
        }).toList();
        repository.saveAll(categories);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(UUID userId, CategoryType type) {
        List<Category> result = type == null
                ? repository.findByUserIdOrderByTypeAscNameAsc(userId)
                : repository.findByUserIdAndTypeOrderByNameAsc(userId, type);
        return result.stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse find(UUID userId, UUID id) {
        return CategoryResponse.from(get(userId, id));
    }

    @Transactional
    public CategoryResponse create(UUID userId, CategoryRequest req) {
        String name = req.name().trim();
        if (repository.existsByUserIdAndTypeAndNameIgnoreCase(userId, req.type(), name)) {
            throw new ConflictException("Já existe uma categoria com este nome.");
        }
        Category c = new Category();
        c.setUserId(userId);
        apply(c, req, name);
        return CategoryResponse.from(repository.save(c));
    }

    @Transactional
    public CategoryResponse update(UUID userId, UUID id, CategoryRequest req) {
        Category c = get(userId, id);
        String name = req.name().trim();
        if (repository.existsByUserIdAndTypeAndNameIgnoreCaseAndIdNot(userId, req.type(), name, id)) {
            throw new ConflictException("Já existe uma categoria com este nome.");
        }
        if (c.getType() != req.type() && transactions.existsByCategoryId(id)) {
            throw new ConflictException("Não é possível alterar o tipo de uma categoria que possui movimentações.");
        }
        apply(c, req, name);
        return CategoryResponse.from(c);
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Category c = get(userId, id);
        if (transactions.existsByCategoryId(id)) {
            throw new ConflictException("Categoria possui movimentações. Arquive-a em vez de excluir.");
        }
        budgets.deleteByCategoryId(id);
        repository.delete(c);
    }

    public Category get(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> NotFoundException.of("Categoria"));
    }

    private void apply(Category c, CategoryRequest req, String name) {
        c.setName(name);
        c.setType(req.type());
        c.setColor(req.color());
        c.setIcon(req.icon());
        if (req.archived() != null) c.setArchived(req.archived());
    }
}
