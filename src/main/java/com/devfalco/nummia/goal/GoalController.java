package com.devfalco.nummia.goal;

import com.devfalco.nummia.goal.dto.ContributionRequest;
import com.devfalco.nummia.goal.dto.ContributionResponse;
import com.devfalco.nummia.goal.dto.GoalRequest;
import com.devfalco.nummia.goal.dto.GoalResponse;
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

@Tag(name = "Metas", description = "Metas financeiras e aportes")
@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
public class GoalController {

    private final GoalService service;
    private final CurrentUser currentUser;

    @GetMapping
    public List<GoalResponse> list(@RequestParam(required = false) GoalStatus status) {
        return service.list(currentUser.id(), status);
    }

    @GetMapping("/{id}")
    public GoalResponse find(@PathVariable UUID id) {
        return service.find(currentUser.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody GoalRequest request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable UUID id, @Valid @RequestBody GoalRequest request) {
        return service.update(currentUser.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(currentUser.id(), id);
    }

    @Operation(summary = "Registra um aporte (valor positivo) ou resgate (valor negativo)")
    @PostMapping("/{id}/contributions")
    public GoalResponse contribute(@PathVariable UUID id, @Valid @RequestBody ContributionRequest request) {
        return service.contribute(currentUser.id(), id, request);
    }

    @GetMapping("/{id}/contributions")
    public List<ContributionResponse> contributions(@PathVariable UUID id) {
        return service.contributions(currentUser.id(), id);
    }
}
