package com.devfalco.nummia.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    int revokeAllByUserId(UUID userId);

    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :before or r.revoked = true")
    int deleteExpiredOrRevoked(Instant before);
}
