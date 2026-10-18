package com.unisal.selectaapi.auth.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.account.service.UserSettingsService;
import com.unisal.selectaapi.auth.dtos.CodeSentDto;
import com.unisal.selectaapi.auth.dtos.DeviceCodeDto;
import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.auth.dtos.PendingLoginDto;
import com.unisal.selectaapi.auth.dtos.TwoFactorVerifyDto;
import com.unisal.selectaapi.auth.entity.LoginChallenge;
import com.unisal.selectaapi.auth.entity.SecurityCodePurpose;
import com.unisal.selectaapi.auth.entity.TrustedDevice;
import com.unisal.selectaapi.auth.repository.LoginChallengeRepository;
import com.unisal.selectaapi.auth.repository.TrustedDeviceRepository;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.mensageria.SecurityMailer;
import com.unisal.selectaapi.user.entity.User;

/**
 * Verificação em duas etapas. Num dispositivo novo, depois da senha, o usuário
 * confirma o acesso com um código enviado ao e-mail ou gerado em outro
 * dispositivo onde já está conectado.
 */
@Service
public class TwoFactorService {

    private static final Duration CHALLENGE_TTL = Duration.ofMinutes(15);
    private static final Duration EMAIL_CODE_TTL = Duration.ofMinutes(10);
    private static final Duration DEVICE_CODE_TTL = Duration.ofMinutes(5);
    private static final Duration DISABLE_CODE_TTL = Duration.ofMinutes(10);
    private static final int MAX_CHALLENGE_ATTEMPTS = 5;

    private final LoginChallengeRepository challengeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserSettingsService settingsService;
    private final SecurityCodeService securityCodeService;
    private final SessionService sessionService;
    private final SecurityMailer mailer;
    private final BCryptPasswordEncoder passwordEncoder;

    public TwoFactorService(LoginChallengeRepository challengeRepository,
            TrustedDeviceRepository trustedDeviceRepository,
            UserSettingsService settingsService,
            SecurityCodeService securityCodeService,
            SessionService sessionService,
            SecurityMailer mailer,
            BCryptPasswordEncoder passwordEncoder) {
        this.challengeRepository = challengeRepository;
        this.trustedDeviceRepository = trustedDeviceRepository;
        this.settingsService = settingsService;
        this.securityCodeService = securityCodeService;
        this.sessionService = sessionService;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean requiresChallenge(User user, ClientInfo client) {
        if (!settingsService.isTwoFactorEnabled(user)) {
            return false;
        }
        String deviceHash = client.deviceIdHash();
        if (deviceHash == null) {
            return true;
        }
        var trusted = trustedDeviceRepository.findByUserIdAndDeviceIdHash(user.getId(), deviceHash);
        trusted.ifPresent(device -> {
            device.touch(LocalDateTime.now());
            trustedDeviceRepository.save(device);
        });
        return trusted.isEmpty();
    }

    @Transactional
    public LoginResponseDto createChallenge(User user, ClientInfo client) {
        var now = LocalDateTime.now();
        var challenge = challengeRepository.save(new LoginChallenge(
                user,
                client.deviceIdHash(),
                client.deviceName(),
                client.userAgent(),
                client.ipAddress(),
                client.location(),
                now,
                now.plus(CHALLENGE_TTL)));
        return LoginResponseDto.challenge(challenge.getId().toString(), user.getRole(),
                SecureTokens.maskEmail(user.getEmail()));
    }

    @Transactional
    public CodeSentDto sendEmailCode(UUID challengeId, ClientInfo client) {
        var challenge = loadPending(challengeId, client);
        var now = LocalDateTime.now();

        if (challenge.getEmailCodeSentAt() != null && challenge.getEmailCodeSentAt()
                .isAfter(now.minusSeconds(SecurityCodeService.RESEND_COOLDOWN_SECONDS))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Aguarde um minuto antes de solicitar um novo código.");
        }

        String code = SecureTokens.numericCode(SecurityCodeService.CODE_DIGITS);
        challenge.attachEmailCode(passwordEncoder.encode(code), now, now.plus(EMAIL_CODE_TTL));
        challengeRepository.save(challenge);

        var user = challenge.getUser();
        mailer.sendCode(
                user.getEmail(),
                "Código de acesso - Selecta",
                "Confirme que é você entrando",
                "Recebemos um login na sua conta. Use o código abaixo para concluir o acesso.",
                code,
                (int) EMAIL_CODE_TTL.toMinutes(),
                describeAttempt(challenge) + " Se não foi você, altere sua senha imediatamente.");

        return new CodeSentDto(SecureTokens.maskEmail(user.getEmail()), (int) EMAIL_CODE_TTL.toMinutes(),
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponseDto verify(TwoFactorVerifyDto dto, ClientInfo client) {
        var challenge = loadPending(dto.challengeId(), client);
        if (challenge.getAttempts() >= MAX_CHALLENGE_ATTEMPTS) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Muitas tentativas incorretas. Faça login novamente.");
        }

        var user = challenge.getUser();
        var now = LocalDateTime.now();
        boolean valid = switch (dto.method()) {
            case EMAIL -> challenge.getEmailCodeHash() != null
                    && challenge.getEmailCodeExpiresAt() != null
                    && challenge.getEmailCodeExpiresAt().isAfter(now)
                    && passwordEncoder.matches(dto.code(), challenge.getEmailCodeHash());
            case DEVICE -> securityCodeService.tryConsume(user, SecurityCodePurpose.LOGIN_DEVICE_APPROVAL, dto.code());
        };

        if (!valid) {
            challenge.registerFailedAttempt();
            challengeRepository.save(challenge);
            int remaining = MAX_CHALLENGE_ATTEMPTS - challenge.getAttempts();
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, remaining > 0
                    ? "Código inválido ou expirado. Você ainda tem " + remaining + " tentativa(s)."
                    : "Muitas tentativas incorretas. Faça login novamente.");
        }

        challenge.complete(now);
        challengeRepository.save(challenge);

        if (Boolean.TRUE.equals(dto.trustDevice())) {
            trust(user, client);
        }

        mailer.sendNotice(
                user.getEmail(),
                "Novo acesso à sua conta - Selecta",
                "Novo dispositivo conectado",
                "Um novo dispositivo concluiu a verificação em duas etapas e entrou na sua conta.",
                describeAttempt(challenge) + " Se não reconhece este acesso, encerre a sessão nas configurações e altere sua senha.");

        return sessionService.startSession(user, client);
    }

    @Transactional(readOnly = true)
    public List<PendingLoginDto> listPending(UUID userId) {
        return challengeRepository
                .findByUserIdAndCompletedAtIsNullAndDeniedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(userId,
                        LocalDateTime.now())
                .stream()
                .map(challenge -> new PendingLoginDto(
                        challenge.getId(),
                        challenge.getDeviceName(),
                        DeviceNameParser.isMobile(challenge.getUserAgent()),
                        challenge.getLocation(),
                        challenge.getIpAddress(),
                        challenge.getCreatedAt(),
                        challenge.getExpiresAt()))
                .toList();
    }

    @Transactional
    public void deny(UUID userId, UUID challengeId) {
        var challenge = challengeRepository.findByIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Tentativa de acesso não encontrada"));
        challenge.deny(LocalDateTime.now());
        challengeRepository.save(challenge);
    }

    @Transactional
    public DeviceCodeDto generateDeviceCode(User user) {
        if (!settingsService.isTwoFactorEnabled(user)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Ative a verificação em duas etapas para gerar códigos de acesso.");
        }
        var issued = securityCodeService.issue(user, SecurityCodePurpose.LOGIN_DEVICE_APPROVAL, null, DEVICE_CODE_TTL, 0);
        return new DeviceCodeDto(issued.code(), issued.expiresAt(), (int) DEVICE_CODE_TTL.toSeconds());
    }

    @Transactional
    public void enable(User user, ClientInfo client) {
        var settings = settingsService.getOrCreate(user);
        if (settings.isTwoFactorEnabled()) {
            return;
        }
        settings.setTwoFactorEnabled(true);
        trust(user, client);

        mailer.sendNotice(
                user.getEmail(),
                "Verificação em duas etapas ativada - Selecta",
                "Sua conta está mais protegida",
                "A verificação em duas etapas foi ativada. A partir de agora, entrar por um dispositivo novo exige um código enviado ao seu e-mail ou gerado em outro dispositivo conectado.",
                null);
    }

    @Transactional
    public CodeSentDto requestDisable(User user) {
        if (!settingsService.isTwoFactorEnabled(user)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "A verificação em duas etapas já está desativada.");
        }
        var issued = securityCodeService.issue(user, SecurityCodePurpose.TWO_FACTOR_DISABLE, null, DISABLE_CODE_TTL,
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
        mailer.sendCode(
                user.getEmail(),
                "Desativar verificação em duas etapas - Selecta",
                "Confirme a desativação da verificação em duas etapas",
                "Use o código abaixo para desativar a verificação em duas etapas da sua conta.",
                issued.code(),
                (int) DISABLE_CODE_TTL.toMinutes(),
                "Se você não pediu isso, ignore este e-mail e altere sua senha.");
        return new CodeSentDto(SecureTokens.maskEmail(user.getEmail()), (int) DISABLE_CODE_TTL.toMinutes(),
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
    }

    @Transactional
    public void disable(User user, String code) {
        securityCodeService.consume(user, SecurityCodePurpose.TWO_FACTOR_DISABLE, code);
        var settings = settingsService.getOrCreate(user);
        settings.setTwoFactorEnabled(false);
        trustedDeviceRepository.deleteByUserId(user.getId());
        securityCodeService.invalidate(user, SecurityCodePurpose.LOGIN_DEVICE_APPROVAL);
    }

    @Transactional(readOnly = true)
    public long countTrustedDevices(UUID userId) {
        return trustedDeviceRepository.countByUserId(userId);
    }

    @Transactional
    public void forgetAll(User user) {
        trustedDeviceRepository.deleteByUserId(user.getId());
        challengeRepository.deleteByUserId(user.getId());
    }

    private void trust(User user, ClientInfo client) {
        String deviceHash = client.deviceIdHash();
        if (deviceHash == null) {
            return;
        }
        var now = LocalDateTime.now();
        var device = trustedDeviceRepository.findByUserIdAndDeviceIdHash(user.getId(), deviceHash)
                .orElseGet(() -> new TrustedDevice(user, deviceHash, client.deviceName(), now));
        device.touch(now);
        trustedDeviceRepository.save(device);
    }

    private LoginChallenge loadPending(UUID challengeId, ClientInfo client) {
        var challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Verificação não encontrada. Faça login novamente."));

        var now = LocalDateTime.now();
        if (challenge.getDeniedAt() != null) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Este acesso foi bloqueado em outro dispositivo conectado à conta.");
        }
        if (!challenge.isPending(now)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "Esta verificação expirou. Faça login novamente.");
        }
        if (challenge.getDeviceIdHash() != null
                && !SecureTokens.constantTimeEquals(challenge.getDeviceIdHash(), client.deviceIdHash())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Conclua a verificação no mesmo dispositivo em que fez o login.");
        }
        return challenge;
    }

    private String describeAttempt(LoginChallenge challenge) {
        var builder = new StringBuilder("Dispositivo: ").append(challenge.getDeviceName());
        if (challenge.getLocation() != null) {
            builder.append(" · ").append(challenge.getLocation());
        }
        if (challenge.getIpAddress() != null) {
            builder.append(" · IP ").append(challenge.getIpAddress());
        }
        return builder.append('.').toString();
    }
}
