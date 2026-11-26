package com.devfalco.nummia.budget;

import com.devfalco.nummia.budget.dto.BudgetRequest;
import com.devfalco.nummia.budget.dto.BudgetResponse;
import com.devfalco.nummia.budget.dto.CopyBudgetsRequest;
import com.devfalco.nummia.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Tag(name = "Orçamentos", description = "Controle de gastos mensais e limites por categoria")
@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService service;
    private final CurrentUser currentUser;

    @Operation(summary = "Lista orçamentos do mês (yyyy-MM; padrão: mês atual) com valores gastos")
    @GetMapping
    public List<BudgetResponse> list(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.list(currentUser.id(), month);
    }

    @GetMapping("/{id}")
    public BudgetResponse find(@PathVariable UUID id) {
        return service.find(currentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable UUID id, @Valid @RequestBody BudgetRequest request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }

    @Operation(summary = "Copia os orçamentos de um mês para outro")
    @PostMapping("/copy")
    public List<BudgetResponse> copy(@Valid @RequestBody CopyBudgetsRequest request) {
        return service.copy(currentUser.id(), request.from(), request.to());
    }
}
