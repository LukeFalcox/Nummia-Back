package com.devfalco.nummia.auth;

import com.devfalco.nummia.audit.AuditService;
import com.devfalco.nummia.auth.dto.AuthResponse;
import com.devfalco.nummia.auth.dto.LoginRequest;
import com.devfalco.nummia.auth.dto.RegisterRequest;
import com.devfalco.nummia.category.CategoryService;
import com.devfalco.nummia.common.exception.ConflictException;
import com.devfalco.nummia.common.exception.UnauthorizedException;
import com.devfalco.nummia.config.NummiaProperties;
import com.devfalco.nummia.security.JwtService;
import com.devfalco.nummia.security.TokenHasher;
import com.devfalco.nummia.user.User;
import com.devfalco.nummia.user.UserRepository;
import com.devfalco.nummia.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CategoryService categoryService;
    private final AuditService audit;
    private final NummiaProperties properties;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = normalize(req.email());
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Já existe uma conta cadastrada com este e-mail.");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        users.save(user);
        categoryService.createDefaults(user.getId());
        audit.log(user.getId(), "USER_REGISTERED");
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(normalize(req.email())).orElse(null);
        if (user == null || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            audit.log(user != null ? user.getId() : null, "LOGIN_FAILED");
            throw new UnauthorizedException("E-mail ou senha inválidos.");
        }
        audit.log(user.getId(), "LOGIN");
        return issueTokens(user);
    }

    /** Troca um refresh token válido por um novo par de tokens (rotação). */
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken token = refreshTokens.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));
        if (token.isRevoked()) {
            // Reuso de token revogado: possível vazamento. Revoga todas as sessões do usuário.
            refreshTokens.revokeAllByUserId(token.getUserId());
            audit.log(token.getUserId(), "REFRESH_TOKEN_REUSE");
            throw new UnauthorizedException("Refresh token inválido.");
        }
        if (!token.isActive()) {
            throw new UnauthorizedException("Refresh token expirado.");
        }
        token.setRevoked(true);
        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Refresh token inválido."));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(TokenHasher.sha256(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            audit.log(token.getUserId(), "LOGOUT");
        });
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        refreshTokens.deleteExpiredOrRevoked(Instant.now());
    }

    private AuthResponse issueTokens(User user) {
        String raw = TokenHasher.newToken();
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setCreatedAt(Instant.now());
        token.setExpiresAt(Instant.now().plus(properties.jwt().refreshTokenTtl()));
        refreshTokens.save(token);
        return new AuthResponse(jwtService.generateAccessToken(user), raw, "Bearer",
                jwtService.accessTokenTtlSeconds(), UserResponse.from(user));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
