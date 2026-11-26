package com.devfalco.nummia.category;

import com.devfalco.nummia.category.dto.CategoryRequest;
import com.devfalco.nummia.category.dto.CategoryResponse;
import com.devfalco.nummia.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

@Tag(name = "Categorias", description = "Categorização de receitas e despesas")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService service;
    private final CurrentUser currentUser;

    @Operation(summary = "Lista categorias (opcionalmente filtradas por tipo)")
    @GetMapping
    public List<CategoryResponse> list(@RequestParam(required = false) CategoryType type) {
        return service.list(currentUser.id(), type);
    }

    @GetMapping("/{id}")
    public CategoryResponse find(@PathVariable UUID id) {
        return service.find(currentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return service.update(currentUser.id(), id, request);
    }

    @Operation(summary = "Exclui uma categoria sem movimentações")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }
}
