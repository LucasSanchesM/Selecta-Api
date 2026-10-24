package com.unisal.selectaapi.services;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.unisal.selectaapi.auth.entity.RefreshToken;
import com.unisal.selectaapi.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.unisal.selectaapi.auth.dtos.VerifyEmailDto;
import com.unisal.selectaapi.auth.entity.VerificationCode;
import com.unisal.selectaapi.auth.service.VerificationCodeService;
import com.unisal.selectaapi.exception.BusinessException;
import com.unisal.selectaapi.mensageria.dto.EmailSendRequest;
import com.unisal.selectaapi.user.UserRepository;
import com.unisal.selectaapi.user.UserRole;
import com.unisal.selectaapi.user.UserService;
import com.unisal.selectaapi.user.dto.CreateUserDto;
import com.unisal.selectaapi.user.dto.UpdateUserDto;
import com.unisal.selectaapi.user.entity.User;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private VerificationCodeService verificationCodeService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Captor
    private ArgumentCaptor<EmailSendRequest> emailSendCaptor;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    @Captor
    private ArgumentCaptor<String> emailArgumentCaptor;

    @Captor
    private ArgumentCaptor<VerificationCode> verificationCodeCaptor;

    @Captor
    private ArgumentCaptor<String> codeCaptor;

    @Captor
    private ArgumentCaptor<EmailSendRequest> emailSendRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<UUID> idCaptor;


    @Nested
    class createUser{
        @Test
        @DisplayName("Should create user successfully")
        void shoulCreateUserWithSucess(){
            //===== ARRANGE =======
            CreateUserDto inputDto = new CreateUserDto("Joao", "joao@email.com", "1234", UserRole.CANDIDATE);
            doReturn(Optional.empty()).when(userRepository).findByEmail(inputDto.email());
            doReturn("criptografado").when(passwordEncoder).encode(inputDto.password());
            
            //em vez de salvar no banco o metodo save irá retornar o proprio parametro passado
            when(userRepository.save(any(User.class))).thenAnswer(chamada -> {
                //pega o primeiro parametro passado entre parewnteses no save
                User userPassadoComoArgumento = chamada.getArgument(0);
                //retorna o mesmo
                return userPassadoComoArgumento;
            });
            

            //===== ACT =======
            var output = userService.createUser(inputDto);

            //===== ASSERT =======
            verify(userRepository).save(userArgumentCaptor.capture());
            verify(rabbitTemplate).convertAndSend(eq("email.exchange"), eq("email.confirmacao.key"), emailSendCaptor.capture());
            verify(verificationCodeService).saveCode(userArgumentCaptor.capture(), codeCaptor.capture());

            List<User> usersCapturados = userArgumentCaptor.getAllValues();

            var userSaved = usersCapturados.get(0);
            var emailSend = emailSendCaptor.getValue();
            var userSendToVerificationCode = usersCapturados.get(1);
            var codeCaptured = codeCaptor.getValue();

            //Verifica se o que foi passado no body da requisição bate com o que seria salvo
            assertEquals(inputDto.name(), userSaved.getName());
            assertEquals(inputDto.role().name(), userSaved.getRole());
            assertEquals(inputDto.email(), userSaved.getEmail());
            assertEquals(inputDto.email(), emailSend.email());

            //Verifica se a senha seria corretamente criptografada e se o estado da conta foi corretamente atribuidp
            assertEquals("criptografado", userSaved.getPasswordHash());
            assertFalse(userSaved.getActive());

            //Verifica se o codigo de verificação e o usuario foram colocados corretamente
            assertNotNull(codeCaptured);
            assertEquals(userSaved, userSendToVerificationCode);

            //Verifica se codigo de verificação que seria salvo no banco bate com o que foi enviado ao usuario
            assertEquals(codeCaptured, emailSend.code());

            //Verifica se o que seria salvo no banco bate com o que retorna na requisição
            assertEquals(userSaved.getId(), output.id());
            assertEquals(userSaved.getName(), output.name());
            assertEquals(userSaved.getEmail(), output.email());
            assertEquals(userSaved.getRole(), output.role());
            assertEquals(userSaved.getActive(), output.active());
            assertEquals(userSaved.getCreatedAt(), output.createdAt());
            assertEquals(userSaved.getUpdatedAt(), output.updatedAt());
        }

        @Test
        @DisplayName("Should not create user when email already exist")
        void shouldDontCreateWhenEmailAlreadyExist(){
            //===== ARRANGE =======
            var userExistence = new User();
            userExistence.setName("Joao");
            userExistence.setEmail("joao@email.com");
            userExistence.setPasswordHash("criptografado");
            CreateUserDto inputDto = new CreateUserDto("Joao", "joao@email.com", "1234", UserRole.CANDIDATE);
            doReturn(Optional.of(userExistence)).when(userRepository).findByEmail(inputDto.email());

            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.createUser(inputDto));

            //===== ASSERT =======
            //verifica se a mensagem de excessao enviada foi a correta
            assertEquals("E-mail já está em uso", exception.getMessage());

            //verifica se o email capturado no find foi o mesmo do input
            verify(userRepository).findByEmail(emailArgumentCaptor.capture());
            var emailReturned = emailArgumentCaptor.getValue();
            assertEquals(inputDto.email(), emailReturned);

            //verifica se nenhum registro seria feito no banco
            verify(userRepository, never()).save(any(User.class));
            
            //verifica se nao haveria email sendo registrados no rabbitMQ
            verifyNoInteractions(rabbitTemplate);
        }
    }

    @Nested
    class verifyEmail{
        @Test
        @DisplayName("Should verify user successfully")
        void shouldVerifyUserSuccessfully(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            User user = new User();
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");
            user.setPasswordHash("criptografado");
            user.setActive(false);
            var expireCodeDate = Instant.now().plusSeconds(10*60);
            var createCodeDate = Instant.now().minusSeconds(10*20);
            var verificationCode = new VerificationCode(user, "12345", expireCodeDate, createCodeDate);
            when(userRepository.save(any(User.class))).thenAnswer(chamado -> {
                User userInseridoComoArgumento = chamado.getArgument(0);
                return userInseridoComoArgumento;
            });
            doReturn(Optional.of(user)).when(userRepository).findByEmail(dto.email());
            doReturn(Optional.of(verificationCode)).when(verificationCodeService).getCode(user);

            //===== ACT =======
            userService.verifyEmail(dto.email(), dto.code());

            //===== ASSERT =======
            
            //verifica o que foi capturado nos metodos findbyEmail e save (o que foi passado no parametro)
            verify(userRepository).findByEmail(emailArgumentCaptor.capture());
            verify(userRepository).save(userArgumentCaptor.capture());
            verify(verificationCodeService).getCode(userArgumentCaptor.capture());
            List<User> usersCaptured = userArgumentCaptor.getAllValues();

            //verifica se o email usado na pesquisa por usuario bate com o passado no dto
            var emailCaptured = emailArgumentCaptor.getValue();
            assertEquals(dto.email(), emailCaptured);
            
            //verifica se a conta do usuario seria corretamente ativada
            var userCaptured = usersCaptured.get(0);
            assertTrue(userCaptured.getActive());

            //verifica se o usuario passaod na verificação de existencia bate com o que foi achado no find
            var userCapturedVerification = usersCaptured.get(1);
            assertEquals(userCaptured, userCapturedVerification);

            //Verifica se as variaveis relacionadas ao codigo de verificação seriam corretamente designadas como nulas
            verify(verificationCodeService).removeCode(user);
        }

        @Test
        @DisplayName("Should not verify user when user email dont searched")
        void shouldNotVerifyUserWhenUserEmailDontSearched(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            doReturn(Optional.empty()).when(userRepository).findByEmail(dto.email());

            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.verifyEmail(dto.email(), dto.code()));

            //===== ASSERT =======

            //verifica se a mensagem de excessão foi lançada e se é a correta
            assertEquals("Usuário não encontrado", exception.getMessage());
            
            //verifica se nenhuma informação seria salva no banco
            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeService, never()).saveCode(any(User.class), anyString());
        }

        @Test
        @DisplayName("Should not verify user when it's activated")
        void shouldNotVerifyUserWhenItsActivated(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            User user = new User();
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");
            user.setPasswordHash("criptografado");
            user.setActive(true);
            doReturn(Optional.of(user)).when(userRepository).findByEmail(dto.email());

            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.verifyEmail(dto.email(), dto.code()));

            //===== ASSERT =======

            //verifica se a mensagem de excessão foi lançada e se é a correta
            assertEquals("Conta já está ativa", exception.getMessage());
            
            //verifica se nenhuma informação seria salva no banco
            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeService, never()).saveCode(any(User.class), anyString());

        }

        @Test
        @DisplayName("should not verify user when verification code is null")
        void shouldNotVerifyUserWhenVerificationCodeIsNull(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            User user = new User();
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");
            user.setPasswordHash("criptografado");
            user.setActive(false);
            doReturn(Optional.of(user)).when(userRepository).findByEmail(dto.email());
            doReturn(Optional.empty()).when(verificationCodeService).getCode(user);
            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.verifyEmail(dto.email(), dto.code()));

            //===== ASSERT =======
            assertEquals("Nenhum código de verificação pendente", exception.getMessage());
            
            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeService, never()).saveCode(any(User.class), anyString());

        }

        @Test
        @DisplayName("Should not verify user when verification code is expired")
        void shouldNotVerifyUserWhenVerificationCodeIsExpired(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            User user = new User();
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");
            user.setPasswordHash("criptografado");
            user.setActive(false);
            var expiredCodeDate = Instant.now().minusSeconds(10*60);
            var createCodeDate = Instant.now().minusSeconds(20*60);
            var verificationCode = new VerificationCode(user, "12345", expiredCodeDate, createCodeDate);
            doReturn(Optional.of(user)).when(userRepository).findByEmail(dto.email());
            doReturn(Optional.of(verificationCode)).when(verificationCodeService).getCode(user);
            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.verifyEmail(dto.email(), dto.code()));

            //===== ASSERT =======
            assertEquals("Código de verificação expirado. Solicite um novo código.", exception.getMessage());
            
            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeService, never()).saveCode(any(User.class), anyString());

        }
        @Test
        @DisplayName("Should not verify user when verification code is invalid")
        void shouldNotVerifyUserWhenVerificationCodeIsInvalid(){
            //===== ARRANGE =======
            VerifyEmailDto dto = new VerifyEmailDto("pessoa@email.com", "12345");
            User user = new User();
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");
            user.setPasswordHash("criptografado");
            user.setActive(false);
            var expireCodeDate = Instant.now().plusSeconds(10*60);
            var createCodeDate = Instant.now().minusSeconds(20*60);
            var verificationCode = new VerificationCode(user, "54321", expireCodeDate, createCodeDate);
            doReturn(Optional.of(user)).when(userRepository).findByEmail(dto.email());
            doReturn(Optional.of(verificationCode)).when(verificationCodeService).getCode(user);

            //===== ACT =======
            var exception = assertThrows(BusinessException.class, () -> userService.verifyEmail(dto.email(), dto.code()));

            //===== ASSERT =======
            assertEquals("Código de verificação inválido", exception.getMessage());
            
            verify(userRepository, never()).save(any(User.class));
            verify(verificationCodeService, never()).saveCode(any(User.class), anyString());

        }
    }

    @Nested
    class resendVerificationCode{
        @Test
        @DisplayName("Should resend verification email succefully")
        void shouldResendVerificationEmailSuccefullly(){
            //===== ARRANGE =======
            var input = "email@teste.com";
            var user = new User();
            user.setEmail("email@teste.com");
            user.setActive(false);
            doReturn(Optional.of(user)).when(userRepository).findByEmail(input);
            doReturn(false).when(verificationCodeService).isRateLimited(user);


            //===== ACT =======
            userService.resendVerificationCode(input);

            //===== ASSERT =======
            //Verifica se o input enviado pelo usuario foi o mesmo colocado no campo email do sistema de filas
            verify(rabbitTemplate).convertAndSend(eq("email.exchange"), eq("email.confirmacao.key"), emailSendRequestArgumentCaptor.capture());
            var emailSend = emailSendRequestArgumentCaptor.getValue();
            assertEquals(input, emailSend.email());

            //Verifica que o codigo gerado e enviado ao email é o mesmo salvo no banco
            verify(verificationCodeService, times(1)).saveCode(userArgumentCaptor.capture(), codeCaptor.capture());
            var codeCaptured = codeCaptor.getValue();
            assertEquals(codeCaptured, emailSend.code());
        }
        @Test
        @DisplayName("Should not resend verification email when email don't exist")
        void shouldNotResendVerificationEmailWhenEmailDontExist(){
            //===== ARRANGE =======
            var input = "email@teste.com";
            doReturn(Optional.empty()).when(userRepository).findByEmail(input);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> userService.resendVerificationCode(input));

            //===== ASSERT =======

            //Verificada se exception enviado foi o correto
            assertEquals("Usuário não encontrado", response.getMessage());

            //Verifica se o email nao foi colocado na fila
            verify(rabbitTemplate, never()).convertAndSend(eq("email.exchange"), eq("email.confirmacao.key"), emailSendRequestArgumentCaptor.capture());
            //Verifica que o codigo nao foi salvo no banco
            verify(verificationCodeService, never()).saveCode(userArgumentCaptor.capture(), codeCaptor.capture());
        }

        @Test
        @DisplayName("Should not resend verification user already activated")
        void shouldNotResendVerificationEmailWhenUserAlreadyActivated(){
            //===== ARRANGE =======
            var input = "email@teste.com";
            var user = new User();
            user.setEmail("email@teste.com");
            user.setActive(true);

            doReturn(Optional.of(user)).when(userRepository).findByEmail(input);
            


            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> userService.resendVerificationCode(input));

            //===== ASSERT =======

            //Verificada se exception enviado foi o correto
            assertEquals("Conta já está ativa", response.getMessage());

            //Verifica se o email nao foi colocado na fila
            verify(rabbitTemplate, never()).convertAndSend(eq("email.exchange"), eq("email.confirmacao.key"), emailSendRequestArgumentCaptor.capture());
            //Verifica que o codigo nao foi salvo no banco
            verify(verificationCodeService, never()).saveCode(userArgumentCaptor.capture(), codeCaptor.capture());
        }

        @Test
        @DisplayName("Should not resend verification verification code is RateLimited")
        void shouldNotResendVerificationEmailWhenVerificationCodeIsRateLimited(){
            //===== ARRANGE =======
            var input = "email@teste.com";
            var user = new User();
            user.setEmail("email@teste.com");
            user.setActive(false);

            doReturn(Optional.of(user)).when(userRepository).findByEmail(input);
            doReturn(true).when(verificationCodeService).isRateLimited(user);



            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> userService.resendVerificationCode(input));

            //===== ASSERT =======

            //Verificada se exception enviado foi o correto
            assertEquals("Código de verificação já enviado no último minuto", response.getMessage());

            //Verifica se o email nao foi colocado na fila
            verify(rabbitTemplate, never()).convertAndSend(eq("email.exchange"), eq("email.confirmacao.key"), emailSendRequestArgumentCaptor.capture());
            //Verifica que o codigo nao foi salvo no banco
            verify(verificationCodeService, never()).saveCode(userArgumentCaptor.capture(), codeCaptor.capture());
        }

    }

    @Nested
    class findAllUsers{

        @Test
        @DisplayName("Should return users succefully")
        void shouldReturnUserSuccefully(){

            //===== ARRANGE =======
            var user1 = new User();
            user1.setName("pessoa1");
            user1.setEmail("pessoa1@teste.com");
            var user2 = new User();
            user2.setName("pessoa2");
            user2.setEmail("pessoa2@teste.com");
            List<User> users = List.of(user1, user2);

            doReturn(users).when(userRepository).findAll();

            //===== ACT =======
            var output = userService.findAllUsers();

            //===== ASSERT =======
            var user1Returned = output.get(0);
            var user2Returned = output.get(1);

            //Verifica que a quantidade de usuarios retornada é a mesma que haveria no banco
            assertEquals(2, output.size());


            //Verifica que os usuarioas que estariam no banco seriam os mesmos retornados
            assertEquals(user1.getName(), user1Returned.name());
            assertEquals(user1.getEmail(), user1Returned.email());

            assertEquals(user2.getName(), user2Returned.name());
            assertEquals(user2.getEmail(), user2Returned.email());
        }

    }

    @Nested
    class findUserById{
        @Test
        @DisplayName("Should return a user succefullly")
        void shouldReturnAUserSuccefully(){

            //===== ARRANGE =======
            var userId = UUID.randomUUID();
            var user = new User();
            user.setId(userId);
            user.setName("pessoa");
            user.setEmail("pessoa@email.com");

            doReturn(Optional.of(user)).when(userRepository).findById(userId);

            //===== ACT =======
            var output = userService.findUserById(userId);

            //===== ASSERT =======

            //Verifica se o usuario capturado com o id é mesmo do id passado
            assertEquals(userId, output.id());

        }

        @Test
        @DisplayName("Should not return a user when userId don't find")
        void shouldNotReturnAUserWhenUserIdDontFind(){

            //===== ARRANGE =======
            var userId = UUID.randomUUID();

            doReturn(Optional.empty()).when(userRepository).findById(userId);

            //===== ACT =======
            var response =  assertThrows(BusinessException.class, () -> userService.findUserById(userId));

            //===== ASSERT =======

            //Verifica se a excessão lançada é a correta
            assertEquals("Usuário não encontrado", response.getMessage());

        }
    }

    @Nested
    class updateUser{
        @Test
        @DisplayName("Should update a user succefully")
        void shouldUpdateAUSerSuccefully(){
            //===== ARRANGE =======
            var userId = UUID.randomUUID();
            var infosToUpdate = new UpdateUserDto("Jairo",
                                                    "jairo@teste.com",
                                                    "123456789",
                                                    UserRole.CANDIDATE,
                                                    true);
            var user = new User();
            user.setId(userId);
            user.setName("pessoa");
            user.setEmail("pessoa@teste.com");
            user.setPasswordHash("criptografado");
            user.setRole("CANDIDATE");
            user.setActive(true);

            doReturn(Optional.of(user)).when(userRepository).findById(userId);
            when(userRepository.save(any(User.class))).thenAnswer(chamado -> {
                var userSaved = chamado.getArgument(0);
                return userSaved;
            });
            
            //===== ACT =======
            var output = userService.updateUser(userId, infosToUpdate);

            //===== ASSERT =======

            //Verifica se o nome as informações de usuarias registradas no banco seriam condizentes com as atualizadas
            verify(userRepository).save(userArgumentCaptor.capture());
            var userCaptured = userArgumentCaptor.getValue();

            assertEquals(infosToUpdate.name(), userCaptured.getName());
            assertEquals(infosToUpdate.email(), userCaptured.getEmail());
            verify(passwordEncoder).encode(infosToUpdate.password());

            //Verifica se o retorno do metodo condiz com informações atualizadas
            assertEquals(infosToUpdate.name(), output.name());
            assertEquals(infosToUpdate.email(), output.email());

            //Verifica se usuario retornado foi o mesmo em que foi solicitado alteração
            assertEquals(userId, output.id());

            //Verifica se o usuario que sofreu alteração é o mesmo em que foi informado o
            assertEquals(userId, userCaptured.getId());
        }

        @Test
        @DisplayName("Should not update a user when user id don't find")
        void shouldNotUpdateAUSerWhenUserIdDontFind(){
            //===== ARRANGE =======
            var userId = UUID.randomUUID();
            var infosToUpdate = new UpdateUserDto("Jairo",
                                                    "jairo@teste.com",
                                                    "123456789",
                                                    UserRole.CANDIDATE,
                                                    true);

            doReturn(Optional.empty()).when(userRepository).findById(userId);
            
            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> userService.updateUser(userId, infosToUpdate));

            //===== ASSERT =======

            //Verifica se a excessao lançada é a correta
            assertEquals("Usuário não encontrado", response.getMessage());
            verify(userRepository, never()).save(any(User.class));

        }

    }


    @Nested
    class deleteUser{
        @Test
        @DisplayName("Should delete a user succefully")
        void shouldDeleteAUserSuccefully(){
            //===== ARRANGE =======
            var userId = UUID.randomUUID();
            var user = new User();
            user.setId(userId);
            user.setName("pessoaTeste");
            user.setEmail("pessoa@teste.com");

            doReturn(Optional.of(user)).when(userRepository).findById(userId);

            //===== ACT =======
            userService.deleteUser(userId);
            
            //===== ASSERT =======

            //Verifica se o id que seria usado para pesquisa bate com o inserido no metodo
            verify(userRepository).findById(idCaptor.capture());
            var idCaptured = idCaptor.getValue();
            assertEquals(userId, idCaptured);

            //Verica se o metodos foram chamados corretamente e quais valores seriam inseridos
            verify(verificationCodeService).removeCode(userArgumentCaptor.capture());
            assertNotNull(user.getExcluidoAt());
        }
        
        @Test
        @DisplayName("Should not delete a user when id don't find")
        void shouldNotDeleteAUserWhenIdDontFind(){
            //===== ARRANGE =======
            var userId = UUID.randomUUID();
            doReturn(Optional.empty()).when(userRepository).findById(userId);

            //===== ACT =======
            var response = assertThrows(BusinessException.class, () -> userService.deleteUser(userId));
            
            //===== ASSERT =======

            //Verifica se a mensagem de excessao lançada é a correta
            assertEquals("Usuário não encontrado", response.getMessage());

            verify(userRepository, never()).delete(any(User.class));
            verify(verificationCodeService, never()).removeCode(any(User.class));

        }



    }
}
