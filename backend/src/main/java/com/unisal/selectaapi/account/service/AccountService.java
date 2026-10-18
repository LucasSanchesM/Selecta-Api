package com.unisal.selectaapi.account.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.unisal.selectaapi.account.dto.AccountSettingsDto;
import com.unisal.selectaapi.account.dto.ChangePasswordDto;
import com.unisal.selectaapi.account.dto.NotificationPreferencesDto;
import com.unisal.selectaapi.account.dto.UpdateAccountProfileDto;
import com.unisal.selectaapi.account.dto.UpdatePrivacyDto;
import com.unisal.selectaapi.account.entity.UserSettings;
import com.unisal.selectaapi.account.repository.UserSettingsRepository;
import com.unisal.selectaapi.auth.dtos.CodeSentDto;
import com.unisal.selectaapi.auth.entity.SecurityCodePurpose;
import com.unisal.selectaapi.auth.service.SecureTokens;
import com.unisal.selectaapi.auth.service.SecurityCodeService;
import com.unisal.selectaapi.auth.service.SessionService;
import com.unisal.selectaapi.auth.service.TwoFactorService;
import com.unisal.selectaapi.candidate.entity.CandidateProfile;
import com.unisal.selectaapi.candidate.repository.CandidateProfileRepository;
import com.unisal.selectaapi.candidate.service.CandidateProfileService;
import com.unisal.selectaapi.candidatura.service.CandidaturaService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.mensageria.SecurityMailer;
import com.unisal.selectaapi.storage.FileStorage;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.entity.User;

import tools.jackson.databind.json.JsonMapper;

@Service
public class AccountService {

    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration EXPORT_COOLDOWN = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final UserSettingsService settingsService;
    private final UserSettingsRepository settingsRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateProfileService candidateProfileService;
    private final CandidaturaService candidaturaService;
    private final SecurityCodeService securityCodeService;
    private final SessionService sessionService;
    private final TwoFactorService twoFactorService;
    private final SecurityMailer mailer;
    private final BCryptPasswordEncoder passwordEncoder;
    private final FileStorage fileStorage;
    private final JsonMapper jsonMapper;

    public AccountService(UserRepository userRepository,
            UserSettingsService settingsService,
            UserSettingsRepository settingsRepository,
            CandidateProfileRepository candidateProfileRepository,
            CandidateProfileService candidateProfileService,
            CandidaturaService candidaturaService,
            SecurityCodeService securityCodeService,
            SessionService sessionService,
            TwoFactorService twoFactorService,
            SecurityMailer mailer,
            BCryptPasswordEncoder passwordEncoder,
            FileStorage fileStorage,
            JsonMapper jsonMapper) {
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.settingsRepository = settingsRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.candidateProfileService = candidateProfileService;
        this.candidaturaService = candidaturaService;
        this.securityCodeService = securityCodeService;
        this.sessionService = sessionService;
        this.twoFactorService = twoFactorService;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.fileStorage = fileStorage;
        this.jsonMapper = jsonMapper;
    }


    @Transactional
    public AccountSettingsDto getSettings(UUID userId) {
        var user = findUser(userId);
        var settings = settingsService.getOrCreate(user);
        var profile = candidateProfileRepository.findByUserId(userId);

        String avatarUrl = profile.map(CandidateProfile::getProfileImageUrl).orElse(null);
        if (avatarUrl == null) {
            avatarUrl = user.getAvatarUrl();
        }
        String phone = profile.map(CandidateProfile::getPhone).filter(value -> !value.isBlank())
                .orElse(settings.getPhone());

        return new AccountSettingsDto(
                new AccountSettingsDto.Account(user.getId(), user.getName(), user.getEmail(), phone,
                        settings.getLanguage(), avatarUrl, user.getRole()),
                toNotifications(settings),
                new AccountSettingsDto.PrivacySettingsDto(settings.getProfileVisibility(), settings.isShowSalary(),
                        settings.isOpenToWork(), settings.isAllowDirectContact()),
                new AccountSettingsDto.Security(
                        settings.isTwoFactorEnabled(),
                        settings.getPasswordChangedAt() != null ? settings.getPasswordChangedAt() : user.getCreatedAt(),
                        twoFactorService.countTrustedDevices(userId),
                        SecureTokens.maskEmail(user.getEmail())));
    }


    @Transactional
    public AccountSettingsDto updateProfile(UUID userId, UpdateAccountProfileDto dto) {
        var user = findUser(userId);
        var settings = settingsService.getOrCreate(user);

        user.setName(dto.name().trim());
        userRepository.save(user);

        String phone = dto.phone() == null || dto.phone().isBlank() ? null : dto.phone().trim();
        settings.setPhone(phone);
        settings.setLanguage(dto.language());
        settingsRepository.save(settings);

        candidateProfileRepository.findByUserId(userId).ifPresent(profile -> {
            profile.setPhone(phone);
            candidateProfileRepository.save(profile);
        });

        return getSettings(userId);
    }

    @Transactional
    public AccountSettingsDto removeAvatar(UUID userId) {
        var user = findUser(userId);
        candidateProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getProfileImageKey() != null) {
                fileStorage.deleteQuietly(profile.getProfileImageKey());
            }
            profile.setProfileImageKey(null);
            profile.setProfileImageUrl(null);
            candidateProfileRepository.save(profile);
        });
        user.setAvatarUrl(null);
        userRepository.save(user);
        return getSettings(userId);
    }

    @Transactional
    public CodeSentDto requestEmailChange(UUID userId, String rawEmail) {
        var user = findUser(userId);
        String newEmail = rawEmail.trim().toLowerCase(Locale.ROOT);

        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Este já é o e-mail da sua conta.");
        }
        if (userRepository.existsByEmail(newEmail)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "E-mail já está em uso");
        }

        var issued = securityCodeService.issue(user, SecurityCodePurpose.EMAIL_CHANGE, newEmail, CODE_TTL,
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
        mailer.sendCode(
                newEmail,
                "Confirme seu novo e-mail - Selecta",
                "Confirme seu novo endereço de e-mail",
                "Recebemos um pedido para usar este endereço na sua conta Selecta. Digite o código abaixo para confirmar.",
                issued.code(),
                (int) CODE_TTL.toMinutes(),
                "Se você não reconhece este pedido, ignore este e-mail.");

        return new CodeSentDto(SecureTokens.maskEmail(newEmail), (int) CODE_TTL.toMinutes(),
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
    }

    @Transactional
    public AccountSettingsDto confirmEmailChange(UUID userId, String code) {
        var user = findUser(userId);
        var securityCode = securityCodeService.consume(user, SecurityCodePurpose.EMAIL_CHANGE, code);
        String newEmail = securityCode.getPayload();

        if (newEmail == null || userRepository.existsByEmail(newEmail)) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "E-mail já está em uso");
        }

        String previousEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);

        mailer.sendNotice(
                previousEmail,
                "O e-mail da sua conta foi alterado - Selecta",
                "Seu e-mail de acesso mudou",
                "O e-mail da sua conta Selecta foi alterado para " + SecureTokens.maskEmail(newEmail) + ".",
                "Se não foi você, entre em contato com o suporte da Selecta imediatamente.");

        return getSettings(userId);
    }

    // ---------------------------------------------------------------- preferências

    @Transactional
    public AccountSettingsDto updateNotifications(UUID userId, NotificationPreferencesDto dto) {
        var settings = settingsService.getOrCreate(findUser(userId));
        settings.setApplicationUpdatesEmail(dto.applicationUpdatesEmail());
        settings.setApplicationUpdatesPush(dto.applicationUpdatesPush());
        settings.setMessagesEmail(dto.messagesEmail());
        settings.setMessagesPush(dto.messagesPush());
        settings.setRecommendedJobsEmail(dto.recommendedJobsEmail());
        settings.setRecommendedJobsPush(dto.recommendedJobsPush());
        settings.setNewsEmail(dto.newsEmail());
        settings.setNewsPush(dto.newsPush());
        settingsRepository.save(settings);
        return getSettings(userId);
    }

    @Transactional
    public AccountSettingsDto updatePrivacy(UUID userId, UpdatePrivacyDto dto) {
        var settings = settingsService.getOrCreate(findUser(userId));
        settings.setProfileVisibility(dto.profileVisibility());
        settings.setShowSalary(dto.showSalary());
        settings.setOpenToWork(dto.openToWork());
        settings.setAllowDirectContact(dto.allowDirectContact());
        settingsRepository.save(settings);
        return getSettings(userId);
    }

    // ---------------------------------------------------------------- senha

    @Transactional
    public CodeSentDto requestPasswordChange(UUID userId) {
        var user = findUser(userId);
        var issued = securityCodeService.issue(user, SecurityCodePurpose.PASSWORD_CHANGE, null, CODE_TTL,
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
        mailer.sendCode(
                user.getEmail(),
                "Código para alterar sua senha - Selecta",
                "Alteração de senha",
                "Use o código abaixo para criar uma nova senha para a sua conta.",
                issued.code(),
                (int) CODE_TTL.toMinutes(),
                "Se você não pediu para alterar a senha, ignore este e-mail — sua senha atual continua valendo.");
        return new CodeSentDto(SecureTokens.maskEmail(user.getEmail()), (int) CODE_TTL.toMinutes(),
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public int changePassword(UUID userId, UUID currentSessionId, ChangePasswordDto dto) {
        var user = findUser(userId);
        if (passwordEncoder.matches(dto.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "A nova senha deve ser diferente da atual.");
        }

        securityCodeService.consume(user, SecurityCodePurpose.PASSWORD_CHANGE, dto.code());

        user.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
        userRepository.save(user);

        var settings = settingsService.getOrCreate(user);
        settings.setPasswordChangedAt(LocalDateTime.now());
        settingsRepository.save(settings);

        int revoked = Boolean.FALSE.equals(dto.signOutOtherSessions())
                ? 0
                : sessionService.revokeOthers(userId, currentSessionId);

        mailer.sendNotice(
                user.getEmail(),
                "Sua senha foi alterada - Selecta",
                "Senha alterada com sucesso",
                "A senha da sua conta Selecta acabou de ser alterada.",
                "Se não foi você, recupere o acesso imediatamente e encerre as sessões desconhecidas.");
        return revoked;
    }

    // ---------------------------------------------------------------- zona de risco

    @Transactional
    public CodeSentDto requestDataExport(UUID userId) {
        var user = findUser(userId);
        var settings = settingsService.getOrCreate(user);
        var now = LocalDateTime.now();

        if (settings.getLastExportRequestedAt() != null
                && settings.getLastExportRequestedAt().isAfter(now.minus(EXPORT_COOLDOWN))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS,
                    "Você já pediu uma cópia há pouco. Aguarde alguns minutos para pedir de novo.");
        }

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("geradoEm", now);
        export.put("conta", Map.of(
                "id", user.getId(),
                "nome", user.getName(),
                "email", user.getEmail(),
                "tipo", user.getRole(),
                "criadaEm", user.getCreatedAt()));
        export.put("configuracoes", getSettings(userId));
        export.put("sessoesAtivas", sessionService.listActive(userId, null));
        export.put("perfilCandidato", safely(() -> candidateProfileService.findProfileByUserId(userId)));
        export.put("candidaturas", safely(() -> candidaturaService.listarMinhasCandidaturas(userId)));

        String json = jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export);
        mailer.sendDataExport(user.getEmail(), user.getName(), json);

        settings.setLastExportRequestedAt(now);
        settingsRepository.save(settings);

        return new CodeSentDto(SecureTokens.maskEmail(user.getEmail()), 0, (int) EXPORT_COOLDOWN.toSeconds());
    }

    @Transactional
    public CodeSentDto requestAccountDeletion(UUID userId) {
        var user = findUser(userId);
        var issued = securityCodeService.issue(user, SecurityCodePurpose.ACCOUNT_DELETION, null, CODE_TTL,
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
        mailer.sendCode(
                user.getEmail(),
                "Confirme a exclusão da sua conta - Selecta",
                "Confirme a exclusão da conta",
                "Use o código abaixo para excluir definitivamente sua conta Selecta, seu perfil, currículos e candidaturas.",
                issued.code(),
                (int) CODE_TTL.toMinutes(),
                "Se você não pediu isso, altere sua senha agora mesmo.");
        return new CodeSentDto(SecureTokens.maskEmail(user.getEmail()), (int) CODE_TTL.toMinutes(),
                SecurityCodeService.RESEND_COOLDOWN_SECONDS);
    }

    /**
     * Exclusão por anonimização: candidaturas e mensagens referenciam o usuário,
     * então os dados pessoais são apagados e a conta fica inutilizável.
     */
    @Transactional
    public void deleteAccount(UUID userId, String code) {
        var user = findUser(userId);
        securityCodeService.consume(user, SecurityCodePurpose.ACCOUNT_DELETION, code);

        String originalEmail = user.getEmail();
        String originalName = user.getName();

        candidateProfileRepository.findByUserId(userId).ifPresent(profile -> {
            if (profile.getProfileImageKey() != null) {
                fileStorage.deleteQuietly(profile.getProfileImageKey());
            }
            if (profile.getResumeKey() != null) {
                fileStorage.deleteQuietly(profile.getResumeKey());
            }
            profile.setHeadline(null);
            profile.setSummary(null);
            profile.setPhone(null);
            profile.setCity(null);
            profile.setState(null);
            profile.setProfileImageUrl(null);
            profile.setProfileImageKey(null);
            profile.setResumeUrl(null);
            profile.setResumeKey(null);
            profile.setResumeText(null);
            profile.setLinkedinUrl(null);
            profile.setGithubUrl(null);
            profile.setPortfolioUrl(null);
            profile.getSkills().clear();
            candidateProfileRepository.save(profile);
        });

        sessionService.revokeAll(userId);
        twoFactorService.forgetAll(user);
        securityCodeService.deleteAll(user);
        settingsRepository.deleteByUserId(userId);

        user.setName("Usuário removido");
        user.setEmail("removido-" + user.getId() + "@removido.selecta.invalid");
        user.setPasswordHash(passwordEncoder.encode(SecureTokens.randomToken(32)));
        user.setActive(false);
        user.setAvatarUrl(null);
        user.setGoogleSubject(null);
        user.setLinkedinSubject(null);
        userRepository.save(user);

        mailer.sendNotice(
                originalEmail,
                "Sua conta foi excluída - Selecta",
                "Conta excluída",
                "Olá, " + originalName + ". Sua conta Selecta e seus dados pessoais foram removidos.",
                "Obrigado por ter usado a Selecta. Você pode criar uma nova conta quando quiser.");
    }

    // ---------------------------------------------------------------- utilitários

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    private NotificationPreferencesDto toNotifications(UserSettings settings) {
        return new NotificationPreferencesDto(
                settings.isApplicationUpdatesEmail(),
                settings.isApplicationUpdatesPush(),
                settings.isMessagesEmail(),
                settings.isMessagesPush(),
                settings.isRecommendedJobsEmail(),
                settings.isRecommendedJobsPush(),
                settings.isNewsEmail(),
                settings.isNewsPush());
    }

    private static Object safely(java.util.function.Supplier<Object> supplier) {
        try {
            return supplier.get();
        } catch (BusinessException exception) {
            return null;
        }
    }
}
