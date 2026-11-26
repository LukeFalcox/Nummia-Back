package com.devfalco.nummia.auth.dto;

import com.devfalco.nummia.user.dto.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
