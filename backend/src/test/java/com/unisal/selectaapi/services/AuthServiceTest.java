package com.unisal.selectaapi.services;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import com.unisal.selectaapi.auth.dtos.LoginRequestDto;
import com.unisal.selectaapi.auth.dtos.LoginResponseDto;
import com.unisal.selectaapi.auth.service.AuthService;
import com.unisal.selectaapi.auth.service.ClientInfo;
import com.unisal.selectaapi.auth.service.SessionService;
import com.unisal.selectaapi.auth.service.TwoFactorService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    
    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private SessionService sessionService;

    @Mock
    private TwoFactorService twoFactorService;

    @Captor
    private ArgumentCaptor<String> emailArgumentCaptor;

    @Nested
    class LoginUser{

        private User user;
        private LoginRequestDto input;

        @BeforeEach
        void setUp() {
            //Pra facilitar usarei o mesmo input e usuario no banco, apenas modificando nos testes
            input = new LoginRequestDto("pessoa@teste.com", "1234", UserRole.CANDIDATE);
            user = new User();
            user.setId(UUID.randomUUID());
            user.setEmail("pessoa@teste.com");
            user.setPasswordHash("criptografado");
            user.setActive(true);
            user.setRole(UserRole.CANDIDATE.name());
        }

        @Test
        @DisplayName("should login a user succefully")
        void shouldLoginAUserSuccefully(){
            //===== ARRANGE =======
            doReturn(Optional.of(user)).when(userRepository).findByEmail(input.email());
            doReturn(true).when(passwordEncoder).matches(input.password(), user.getPasswordHash());
            doReturn(false).when(twoFactorService).requiresChallenge(any(User.class), any(ClientInfo.class));
            doReturn(LoginResponseDto.tokens("token-fake", 900L, user.getRole(), "refresh-fake"))
                    .when(sessionService).startSession(any(User.class), any(ClientInfo.class));

            //===== ACT =======
            var response = authService.loginUser(input);

            //===== ARSSERT =======

            //Verifica se find foi chamado apenas uma vez e coleta email usado pra pesquisa
            verify(userRepository, times(1)).findByEmail(emailArgumentCaptor.capture());
            //Verifica se password foi chamado corretamente
            verify(passwordEncoder, times(1)).matches( input.password(), user.getPasswordHash());

            //Verifica se a procura de email foi chamada com o email correto
            var emailUsedToSearch = emailArgumentCaptor.getValue();
            assertEquals(input.email(), emailUsedToSearch);

            //verifica se token seria emitido corretamente
            assertEquals("token-fake", response.tokenJwt());
            assertEquals("refresh-fake", response.refreshToken());
        }

        @Test
        @DisplayName("should require two-factor on an untrusted device")
        void shouldRequireTwoFactorOnUntrustedDevice(){
            doReturn(Optional.of(user)).when(userRepository).findByEmail(input.email());
            doReturn(true).when(passwordEncoder).matches(input.password(), user.getPasswordHash());
            doReturn(true).when(twoFactorService).requiresChallenge(any(User.class), any(ClientInfo.class));
            doReturn(LoginResponseDto.challenge(UUID.randomUUID().toString(), user.getRole(), "pe•••@teste.com"))
                    .when(twoFactorService).createChallenge(any(User.class), any(ClientInfo.class));

            var response = authService.loginUser(input);

            assertEquals(Boolean.TRUE, response.twoFactorRequired());
            assertEquals(null, response.tokenJwt());
            verify(sessionService, never()).startSession(any(User.class), any(ClientInfo.class));
        }

        @Test
        @DisplayName("should not login when email dont exist")
        void shouldNotLoginWhenEmailDontExist(){

            //===== ARRANGE =======
            doReturn(Optional.empty()).when(userRepository).findByEmail(input.email());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> authService.loginUser(input));

            //===== ASSERT =======
            //Verifica se a mensagem de excessão lançada é a correta e se o jwt encoder nao foi chamado
            assertEquals("Email ou senha inválidos", response.getMessage());
            verify(jwtEncoder, never()).encode(any(JwtEncoderParameters.class));

            //verifica se a senha do usuario nunca foi comparada a inserida
            verify(passwordEncoder, never()).matches(any(), any());
        }

        @Test
        @DisplayName("should not login when password is wrong")
        void shouldNotLoginWhenPasswordIsWrong(){

            //===== ARRANGE =======

            doReturn(Optional.of(user)).when(userRepository).findByEmail(input.email());
            doReturn(false).when(passwordEncoder).matches(input.password(), user.getPasswordHash());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> authService.loginUser(input));

            //===== ASSERT =======

            //Verifica se a mensagem de excessão lançada é a correta e se o jwt encoder nao foi chamado
            assertEquals("Email ou senha inválidos", response.getMessage());
            verify(jwtEncoder, never()).encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("should not login when role dont match")
        void shouldNotLoginWhenRoleDontMatch(){

            //===== ARRANGE =======
            user.setRole(UserRole.COMPANY.name());

            doReturn(Optional.of(user)).when(userRepository).findByEmail(input.email());
            doReturn(true).when(passwordEncoder).matches(input.password(), user.getPasswordHash());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> authService.loginUser(input));

            //===== ASSERT =======

            //Verifica se a mensagem de excessão lançada é a correta e se o jwt encoder nao foi chamado
            assertEquals("Esta conta não pertence a este tipo de acesso", response.getMessage());
            verify(jwtEncoder, never()).encode(any(JwtEncoderParameters.class));
        }

        @Test
        @DisplayName("should not login when user isn't activated")
        void shouldNotLoginWhenUserIsNotActivated(){

            //===== ARRANGE =======
            user.setActive(false);

            doReturn(Optional.of(user)).when(userRepository).findByEmail(input.email());
            doReturn(true).when(passwordEncoder).matches(input.password(), user.getPasswordHash());

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> authService.loginUser(input));

            //===== ASSERT =======

            //Verifica se a mensagem de excessão lançada é a correta e se o jwt encoder nao foi chamado
            assertEquals("Conta não verificada. Verifique seu email para ativar sua conta.", response.getMessage());
            verify(jwtEncoder, never()).encode(any(JwtEncoderParameters.class));
        }
    }
}
