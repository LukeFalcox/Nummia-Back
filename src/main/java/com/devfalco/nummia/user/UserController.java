package com.devfalco.nummia.user;

import com.devfalco.nummia.security.CurrentUser;
import com.devfalco.nummia.user.dto.ChangePasswordRequest;
import com.devfalco.nummia.user.dto.DeleteAccountRequest;
import com.devfalco.nummia.user.dto.UpdateProfileRequest;
import com.devfalco.nummia.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuário", description = "Perfil e configurações do usuário autenticado")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final CurrentUser currentUser;

    @Operation(summary = "Dados do usuário autenticado")
    @GetMapping
    public UserResponse me() {
        return service.me(currentUser.id());
    }

    @Operation(summary = "Atualiza perfil e preferências")
    @PutMapping
    public UserResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return service.update(currentUser.id(), request);
    }

    @Operation(summary = "Altera a senha (revoga todas as sessões)")
    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(currentUser.id(), request);
    }

    @Operation(summary = "Exclui definitivamente a conta e todos os dados (LGPD)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@Valid @RequestBody DeleteAccountRequest request) {
        service.delete(currentUser.id(), request.password());
    }
}
