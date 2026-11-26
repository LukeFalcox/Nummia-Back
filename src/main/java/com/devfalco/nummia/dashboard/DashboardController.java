package com.devfalco.nummia.dashboard;

import com.devfalco.nummia.dashboard.dto.DashboardResponse;
import com.devfalco.nummia.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@Tag(name = "Dashboard", description = "Indicadores consolidados para a tela inicial")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;
    private final CurrentUser currentUser;

    @Operation(summary = "Indicadores do mês (yyyy-MM; padrão: mês atual)")
    @GetMapping
    public DashboardResponse dashboard(@RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.build(currentUser.id(), month);
    }
}
