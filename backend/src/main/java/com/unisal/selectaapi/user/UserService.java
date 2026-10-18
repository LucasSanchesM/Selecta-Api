package com.unisal.selectaapi.user;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.unisal.selectaapi.auth.repository.RefreshTokenRepository;
import com.unisal.selectaapi.auth.service.RefreshTokenService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.auth.service.VerificationCodeService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.mensageria.dto.EmailSendRequest;
import com.unisal.selectaapi.user.dto.CreateUserDto;
import com.unisal.selectaapi.user.dto.UpdateUserDto;
import com.unisal.selectaapi.user.dto.UserResponseDto;
import com.unisal.selectaapi.user.entity.User;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RabbitTemplate rabbitTemplate;
    private final VerificationCodeService verificationCodeService;
    private final RefreshTokenRepository refreshTokenRepository;

    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder,
                       RabbitTemplate rabbitTemplate, VerificationCodeService verificationCodeService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.rabbitTemplate = rabbitTemplate;
        this.verificationCodeService = verificationCodeService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public UserResponseDto createUser(CreateUserDto dto) {
        var existingUser = userRepository.findByEmail(dto.email());

        if (existingUser.isPresent()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "E-mail já está em uso");
        }

        UserRole role = dto.role() != null ? dto.role() : UserRole.CANDIDATE;

        if (role == UserRole.ADMIN) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Não é permitido se cadastrar como ADMIN diretamente");
        }
        if (role == UserRole.COMPANY) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "O cadastro empresarial deve ser concluído pelo fluxo de empresas");
        }

        String verificationCode = generateVerificationCode();

        var user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setRole(role.name());
        user.setActive(false);

        var savedUser = userRepository.save(user);
        verificationCodeService.saveCode(savedUser, verificationCode);

        // Publica mensagem no RabbitMQ para enviar o email com o código
        var emailRequest = new EmailSendRequest(verificationCode, dto.email());
        rabbitTemplate.convertAndSend("email.exchange", "email.confirmacao.key", emailRequest);

        return mapToResponse(savedUser);
    }

    @Transactional
    public void verifyEmail(String email, String code) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getActive()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Conta já está ativa");
        }

        var verificationCode = verificationCodeService.getCode(user)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                        "Nenhum código de verificação pendente"));

        if (verificationCode.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Código de verificação expirado. Solicite um novo código.");
        }

        if (!verificationCode.getCode().equals(code)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Código de verificação inválido");
        }

        user.setActive(true);
        userRepository.save(user);
        verificationCodeService.removeCode(user);
    }

    @Transactional
    public void resendVerificationCode(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getActive()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Conta já está ativa");
        }
        if (verificationCodeService.isRateLimited(user)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Código de verificação já enviado no último minuto");
        }

        String newCode = generateVerificationCode();
        verificationCodeService.saveCode(user, newCode);

        var emailRequest = new EmailSendRequest(newCode, email);
        rabbitTemplate.convertAndSend("email.exchange", "email.confirmacao.key", emailRequest);
    }

    public List<UserResponseDto> findAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public UserResponseDto findUserById(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        return mapToResponse(user);
    }

    public UserResponseDto findCurrentUser(UUID id) {
        return findUserById(id);
    }

    public UserResponseDto updateUser(UUID id, UpdateUserDto dto) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (dto.name() != null && !dto.name().isBlank()) {
            user.setName(dto.name());
        }

        if (dto.email() != null && !dto.email().isBlank()) {
            if (!user.getEmail().equals(dto.email()) && userRepository.existsByEmail(dto.email())) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "E-mail já está em uso");
            }
            user.setEmail(dto.email());
        }

        if (dto.password() != null && !dto.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(dto.password()));
        }

        if (dto.role() != null) {
            user.setRole(dto.role().name());
        }

        if (dto.active() != null) {
            user.setActive(dto.active());
        }

        var updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(UUID id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        verificationCodeService.removeCode(user);
        user.setEmail("excluido_" + user.getId() + "@removed.invalid");
        user.setName("Usuário removido");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(false);
        user.setExcluidoAt(Instant.now());
        user.setGoogleSubject(null);
        user.setLinkedinSubject(null);
        refreshTokenRepository.revokeAllByUserId(id);

    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        int code = 10000 + random.nextInt(90000); // Gera número entre 10000 e 99999
        return String.valueOf(code);
    }

    private UserResponseDto mapToResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getActive(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}