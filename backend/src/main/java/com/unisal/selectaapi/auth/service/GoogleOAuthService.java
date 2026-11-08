package com.unisal.selectaapi.auth.service;

import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.entity.User;

import jakarta.transaction.Transactional;

@Service("google")
public class GoogleOAuthService implements OAuthProviderService{
    

    private final UserRepository userRepository;
    private final VerificationCodeService verificationCodeService;
    private final BCryptPasswordEncoder passwordEncoder;

    

    public GoogleOAuthService(UserRepository userRepository, VerificationCodeService verificationCodeService,
            BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationCodeService = verificationCodeService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    @Override 
    public User authenticateCandidate(OidcUser oidcUser){
        String subject = requiredClaim(oidcUser.getSubject(), "Google nao retornou um subject");

        //Primeiro eu vejo se há algum perfil associado a esse subject
        var googleAccount = userRepository.findByGoogleSubject(subject);
        if(googleAccount.isPresent()){
            User user = googleAccount.get();
            verifyCandidateRole(user);
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())
                    && oidcUser.getPicture() != null && !oidcUser.getPicture().isBlank()) {
                user.setAvatarUrl(oidcUser.getPicture().trim());
                userRepository.save(user);
            }
            return user;
        }
        String email = requiredClaim(oidcUser.getEmail(), "Google nao retornou seu email");
        if(!Boolean.TRUE.equals(oidcUser.getEmailVerified())){
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "Apenas candidatos podem utiizar login via google");
        }
        
        email = email.trim().toLowerCase(Locale.ROOT);

        var existingUser = userRepository.findByEmail(email);
        if(existingUser.isPresent()){
            User user = existingUser.get();
            verifyCandidateRole(user);
            user.setGoogleSubject(subject);
            if ((user.getAvatarUrl() == null || user.getAvatarUrl().isBlank())
                    && oidcUser.getPicture() != null && !oidcUser.getPicture().isBlank()) {
                user.setAvatarUrl(oidcUser.getPicture().trim());
            }
            if(!Boolean.TRUE.equals(user.getActive())){
                user.setActive(true);
                verificationCodeService.removeCode(user);
            }
            User savedUser = userRepository.save(user);
            return savedUser;
        }

        User user = new User();
        user.setName(requiredClaim(oidcUser.getFullName(), "Google nao retornou seu nome completo"));
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setActive(true);
        user.setRole(UserRole.CANDIDATE.name());
        user.setGoogleSubject(subject);
        user.setAvatarUrl(oidcUser.getPicture());
        User savedUser = userRepository.save(user);
        return savedUser;
    }

    //Esse metodo daqui ele verifica se a role do candidato utilizado é o correto
    private void verifyCandidateRole(User user){
        if(!"CANDIDATE".equals(user.getRole())){
            throw new BusinessException(HttpStatus.FORBIDDEN, "Apenas candidatos podem utiizar login via google");
        }
    }

    //Esse metodo verifica se claim retornado possuir valor
    private String requiredClaim(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, message);
        }
        return value.trim();
    }
}
