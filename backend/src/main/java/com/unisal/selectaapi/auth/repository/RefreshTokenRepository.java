package com.unisal.selectaapi.auth.repository;

import com.unisal.selectaapi.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByRefreshToken(String refreshToken);

    @Modifying
    @Query("""
        UPDATE RefreshToken m 
            SET m.revogado = true
            WHERE m.user.id  = :user_id
            AND m.revogado = false 
    """)
    void revokeAllByUserId(@Param("user_id") UUID userId);
}
