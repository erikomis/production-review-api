package com.client.productionreview.service;

import com.client.productionreview.dtos.auth.AuthSignInDTORequest;
import com.client.productionreview.dtos.auth.AuthSignUpDTORequest;
import com.client.productionreview.dtos.auth.AuthUpdatePasswordDTORequest;
import com.client.productionreview.dtos.auth.ForgotPasswordRequest;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.MailIntegration;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.model.redis.UserActivationToken;
import com.client.productionreview.model.redis.UserRecoveryCode;
import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.repositories.redis.UserActivationTokenRepository;
import com.client.productionreview.repositories.redis.UserRecoveryCodeRepository;
import com.client.productionreview.service.impl.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String FRONTEND_URL = "https://projetos-web.com";

    @Mock
    private UserRepository userRepository;
    @Mock
    private MailIntegration mailIntegration;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private UserRecoveryCodeRepository userRecoveryCodeRepository;
    @Mock
    private UserActivationTokenRepository userActivationTokenRepository;
    @Mock
    private RoleRepository roleRepository;

    private UserDetailsServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        service = new UserDetailsServiceImpl(userRepository, mailIntegration, passwordEncoder, jwtProvider,
                userRecoveryCodeRepository, userActivationTokenRepository, roleRepository,
                20, List.of(ALLOWED_ORIGIN), FRONTEND_URL);

        user = User.builder()
                .id(1L)
                .username("john")
                .email("john@mail.com")
                .password("hashed")
                .active(true)
                .build();
    }

    private AuthSignInDTORequest signIn(String password) {
        return AuthSignInDTORequest.builder().username("john").password(password).build();
    }

    // ---------- sign-in ----------

    @Test
    void signIn_activeUserWithCorrectPassword_returnsTokens() {
        var access = ResponseCookie.from("token", "a").build();
        var refresh = ResponseCookie.from("refresh_token", "r").build();
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtProvider.generateToken(1L)).thenReturn(access);
        when(jwtProvider.generateRefreshToken(1L)).thenReturn(refresh);

        var response = service.loadUserByUsernameAndPass(signIn("secret"), ALLOWED_ORIGIN);

        assertSame(access, response.getToken());
        assertSame(refresh, response.getRefreshToken());
        verifyNoInteractions(mailIntegration);
    }

    @Test
    void signIn_inactiveUser_isRejectedAndActivationEmailResent() {
        user.setActive(false);
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        GlobalException ex = assertThrows(GlobalException.class,
                () -> service.loadUserByUsernameAndPass(signIn("secret"), ALLOWED_ORIGIN));

        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
        verify(userActivationTokenRepository).save(any(UserActivationToken.class));
        verify(mailIntegration).send(eq("john@mail.com"), contains(ALLOWED_ORIGIN + "/activate-account/"), anyString());
        verify(jwtProvider, never()).generateToken(any());
    }

    @Test
    void signIn_wrongPassword_returnsUnauthorized() {
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        GlobalException ex = assertThrows(GlobalException.class,
                () -> service.loadUserByUsernameAndPass(signIn("wrong"), ALLOWED_ORIGIN));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void signIn_inactiveUserWithWrongPassword_doesNotSendEmail() {
        user.setActive(false);
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThrows(GlobalException.class, () -> service.loadUserByUsernameAndPass(signIn("wrong"), ALLOWED_ORIGIN));

        verifyNoInteractions(mailIntegration);
    }

    @Test
    void signIn_unknownUser_returnsUnauthorized() {
        when(userRepository.findByUsernameOrEmail("john", "john")).thenReturn(Optional.empty());

        GlobalException ex = assertThrows(GlobalException.class,
                () -> service.loadUserByUsernameAndPass(signIn("secret"), ALLOWED_ORIGIN));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    // ---------- sign-up ----------

    @Test
    void signUp_createsInactiveUserWithEncodedPasswordAndSendsActivation() {
        Role role = new Role();
        role.setName("USER");
        when(userRepository.findByUsernameOrEmail("john", "john@mail.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("secret")).thenReturn("encoded");

        service.signUp(AuthSignUpDTORequest.builder()
                .name("John").username("john").email("john@mail.com").password("secret").build(), ALLOWED_ORIGIN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertFalse(saved.getActive());
        assertEquals("encoded", saved.getPassword());
        assertTrue(saved.getRoles().contains(role));

        ArgumentCaptor<UserActivationToken> tokenCaptor = ArgumentCaptor.forClass(UserActivationToken.class);
        verify(userActivationTokenRepository).save(tokenCaptor.capture());
        assertEquals("john@mail.com", tokenCaptor.getValue().getEmail());
        assertNotNull(tokenCaptor.getValue().getToken());
        verify(mailIntegration).send(eq("john@mail.com"), contains(tokenCaptor.getValue().getToken()), anyString());
    }

    @Test
    void signUp_duplicatedUser_throwsConflict() {
        when(userRepository.findByUsernameOrEmail("john", "john@mail.com")).thenReturn(Optional.of(user));

        assertThrows(BusinessExcepion.class, () -> service.signUp(AuthSignUpDTORequest.builder()
                .name("John").username("john").email("john@mail.com").password("secret").build(), ALLOWED_ORIGIN));

        verify(userRepository, never()).save(any());
    }

    @Test
    void activationLink_ignoresOriginNotInAllowList() {
        Role role = new Role();
        role.setName("USER");
        when(userRepository.findByUsernameOrEmail(anyString(), anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));

        service.signUp(AuthSignUpDTORequest.builder()
                .name("John").username("john").email("john@mail.com").password("secret").build(), "https://evil.example.com");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailIntegration).send(eq("john@mail.com"), body.capture(), anyString());
        assertTrue(body.getValue().contains(FRONTEND_URL + "/activate-account/"));
        assertFalse(body.getValue().contains("evil.example.com"));
    }

    // ---------- ativação ----------

    @Test
    void activate_validToken_activatesUserAndConsumesToken() {
        user.setActive(false);
        var token = UserActivationToken.builder().token("tkn").email("john@mail.com").build();
        when(userActivationTokenRepository.findById("tkn")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));

        service.activeUserByRecoveryCode("tkn");

        assertTrue(user.getActive());
        verify(userRepository).save(user);
        verify(userActivationTokenRepository).delete(token);
    }

    @Test
    void activate_unknownToken_throwsNotFound() {
        when(userActivationTokenRepository.findById("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.activeUserByRecoveryCode("nope"));
    }

    @Test
    void recoveryCodeDoesNotOverwriteActivationToken() {
        // o código de recuperação usa um repositório separado do token de ativação
        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.empty());

        service.sendRecoveryCode(new ForgotPasswordRequest("john@mail.com"));

        verifyNoInteractions(userActivationTokenRepository);
    }

    // ---------- recuperação de senha ----------

    @Test
    void sendRecoveryCode_generatesSixDigitCodeWithTtl() {
        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.empty());

        service.sendRecoveryCode(new ForgotPasswordRequest("john@mail.com"));

        ArgumentCaptor<UserRecoveryCode> captor = ArgumentCaptor.forClass(UserRecoveryCode.class);
        verify(userRecoveryCodeRepository).save(captor.capture());
        UserRecoveryCode saved = captor.getValue();
        assertTrue(saved.getCode().matches("\\d{6}"), saved.getCode());
        assertEquals(20 * 60L, saved.getTtl());
        assertEquals(0, saved.getFailedAttempts());
        verify(mailIntegration).send(eq("john@mail.com"), contains(saved.getCode()), anyString());
    }

    @Test
    void sendRecoveryCode_unknownEmail_throwsNotFound() {
        when(userRepository.findByEmail("x@mail.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.sendRecoveryCode(new ForgotPasswordRequest("x@mail.com")));
        verifyNoInteractions(mailIntegration);
    }

    private UserRecoveryCode recoveryCode(LocalDateTime createdDate, int failedAttempts) {
        return UserRecoveryCode.builder()
                .email("john@mail.com")
                .code("123456")
                .createdDate(createdDate)
                .failedAttempts(failedAttempts)
                .build();
    }

    @Test
    void recoveryCodeIsValid_correctCode() {
        when(userRecoveryCodeRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(recoveryCode(LocalDateTime.now(), 0)));

        assertTrue(service.recoveryCodeIsValid("123456", "john@mail.com"));
    }

    @Test
    void recoveryCodeIsValid_expiredCode_isDeleted() {
        var code = recoveryCode(LocalDateTime.now().minusMinutes(21), 0);
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(code));

        assertFalse(service.recoveryCodeIsValid("123456", "john@mail.com"));
        verify(userRecoveryCodeRepository).delete(code);
    }

    @Test
    void recoveryCodeIsValid_wrongCode_incrementsAttempts() {
        var code = recoveryCode(LocalDateTime.now(), 0);
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(code));

        assertFalse(service.recoveryCodeIsValid("000000", "john@mail.com"));
        assertEquals(1, code.getFailedAttempts());
        verify(userRecoveryCodeRepository).save(code);
    }

    @Test
    void recoveryCodeIsValid_tooManyAttempts_invalidatesCode() {
        var code = recoveryCode(LocalDateTime.now(), 4);
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(code));

        assertFalse(service.recoveryCodeIsValid("000000", "john@mail.com"));
        verify(userRecoveryCodeRepository).delete(code);
    }

    @Test
    void recoveryCodeIsValid_nullCode_isInvalid() {
        when(userRecoveryCodeRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(recoveryCode(LocalDateTime.now(), 0)));

        assertFalse(service.recoveryCodeIsValid(null, "john@mail.com"));
    }

    @Test
    void updatePassword_validCode_updatesPasswordAndConsumesCode() {
        var code = recoveryCode(LocalDateTime.now(), 0);
        when(userRecoveryCodeRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(code));
        when(userRepository.findByEmail("john@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");

        service.updatePasswordByRecoveryCode(new AuthUpdatePasswordDTORequest("john@mail.com", "newpass", "123456"));

        assertEquals("new-hash", user.getPassword());
        verify(userRepository).save(user);
        verify(userRecoveryCodeRepository).delete(code);
    }

    @Test
    void updatePassword_invalidCode_throwsBadRequest() {
        when(userRecoveryCodeRepository.findByEmail("john@mail.com"))
                .thenReturn(Optional.of(recoveryCode(LocalDateTime.now(), 0)));

        assertThrows(BadRequestException.class, () -> service.updatePasswordByRecoveryCode(
                new AuthUpdatePasswordDTORequest("john@mail.com", "newpass", "999999")));
        verify(userRepository, never()).save(any());
    }

    // ---------- refresh / logout ----------

    @Test
    void refreshToken_validRefreshCookie_issuesNewTokens() {
        var request = new MockHttpServletRequest();
        when(jwtProvider.getRefreshTokenFromCookie(request)).thenReturn("refresh");
        when(jwtProvider.getUserIdFromRefreshToken("refresh")).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtProvider.generateToken(1L)).thenReturn(ResponseCookie.from("token", "a").build());
        when(jwtProvider.generateRefreshToken(1L)).thenReturn(ResponseCookie.from("refresh_token", "r").build());

        var response = service.refreshToken(request);

        assertEquals("a", response.getToken().getValue());
        assertEquals("r", response.getRefreshToken().getValue());
    }

    @Test
    void refreshToken_invalidToken_throwsUnauthorized() {
        var request = new MockHttpServletRequest();
        when(jwtProvider.getRefreshTokenFromCookie(request)).thenReturn("bad");
        when(jwtProvider.getUserIdFromRefreshToken("bad")).thenReturn(null);

        GlobalException ex = assertThrows(GlobalException.class, () -> service.refreshToken(request));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getHttpStatus());
    }

    @Test
    void logout_returnsExpiredCookies() {
        when(jwtProvider.cleanToken()).thenReturn(ResponseCookie.from("token", "").maxAge(0).build());
        when(jwtProvider.cleanRefreshToken()).thenReturn(ResponseCookie.from("refresh_token", "").maxAge(0).build());

        var cookies = service.logout();

        assertEquals(0, cookies.get("token").getMaxAge().getSeconds());
        assertEquals(0, cookies.get("refreshToken").getMaxAge().getSeconds());
    }
}
