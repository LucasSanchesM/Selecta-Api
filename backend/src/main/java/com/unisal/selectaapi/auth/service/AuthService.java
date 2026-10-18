package com.unisal.selectaapi.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.unisal.selectaapi.auth.dtos.LoginRequestDto;
import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SessionService sessionService;
    private final TwoFactorService twoFactorService;

    public AuthService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
            SessionService sessionService, TwoFactorService twoFactorService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionService = sessionService;
        this.twoFactorService = twoFactorService;
    }

    public LoginResponseDto loginUser(LoginRequestDto dto) {
        return loginUser(dto, ClientInfo.unknown());
    }

    public LoginResponseDto loginUser(LoginRequestDto dto, ClientInfo client) {
        var user = userRepository.findByEmail(dto.email());

        if (user.isEmpty() || !passwordEncoder.matches(dto.password(), user.get().getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Email ou senha inválidos");
        }

        if (!user.get().getRole().equals(dto.role().name())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Esta conta não pertence a este tipo de acesso");
        }

        if (!user.get().getActive()) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Conta não verificada. Verifique seu email para ativar sua conta.");
        }

        if (twoFactorService.requiresChallenge(user.get(), client)) {
            return twoFactorService.createChallenge(user.get(), client);
        }

        return sessionService.startSession(user.get(), client);
    }

    /** Login social: o provedor já autenticou a pessoa, então a sessão nasce direto. */
    public LoginResponseDto issueToken(User user, ClientInfo client) {
        return sessionService.startSession(user, client);
    }
}
