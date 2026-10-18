package com.unisal.selectaapi.auth.service;

import com.unisal.selectaapi.auth.entity.RefreshToken;
import com.unisal.selectaapi.auth.repository.RefreshTokenRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private Duration refreshTokenExpirationDays = Duration.ofDays(7); //1 semana

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user){
        var token = new RefreshToken();
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationDays.toMillis()));
        token.setRefreshToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(token);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RefreshToken rotateRefreshToken(String refreshToken){
        var registro = refreshTokenRepository.findByRefreshToken(refreshToken).
                            orElseThrow(()-> new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token invalido"));

        if(registro.isRevogado()){
            refreshTokenRepository.revokeAllByUserId(registro.getUser().getId());
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token invalido para rotação");
        }

        if(isTokenExpired(registro)){
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Refresh token invalido para rotação");
        }

        registro.setRevogado(true);
        return createRefreshToken(registro.getUser());
    }

    public boolean isTokenExpired(RefreshToken refreshToken){
        return refreshToken.getExpiryDate().isBefore(Instant.now());
    }
}
