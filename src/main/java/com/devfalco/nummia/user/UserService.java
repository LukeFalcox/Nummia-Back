package com.devfalco.nummia.user;

import com.devfalco.nummia.audit.AuditService;
import com.devfalco.nummia.auth.RefreshTokenRepository;
import com.devfalco.nummia.common.exception.BusinessException;
import com.devfalco.nummia.common.exception.NotFoundException;
import com.devfalco.nummia.user.dto.ChangePasswordRequest;
import com.devfalco.nummia.user.dto.UpdateProfileRequest;
import com.devfalco.nummia.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public UserResponse me(UUID userId) {
        return UserResponse.from(get(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest req) {
        User user = get(userId);
        user.setName(req.name().trim());
        if (req.currency() != null) user.setCurrency(req.currency());
        user.setMonthlyIncome(req.monthlyIncome());
        if (req.aiAssistantEnabled() != null) user.setAiAssistantEnabled(req.aiAssistantEnabled());
        if (req.notificationsEnabled() != null) user.setNotificationsEnabled(req.notificationsEnabled());
        audit.log(userId, "PROFILE_UPDATED");
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = get(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta.");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokens.revokeAllByUserId(userId);
        audit.log(userId, "PASSWORD_CHANGED");
    }

    /** Exclusão definitiva da conta e de todos os dados (direito de eliminação - LGPD). */
    @Transactional
    public void delete(UUID userId, String password) {
        User user = get(userId);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException("Senha incorreta.");
        }
        users.delete(user);
        audit.log(null, "ACCOUNT_DELETED", "User", userId, null);
    }

    public User get(UUID userId) {
        return users.findById(userId).orElseThrow(() -> NotFoundException.of("Usuário"));
    }
}
