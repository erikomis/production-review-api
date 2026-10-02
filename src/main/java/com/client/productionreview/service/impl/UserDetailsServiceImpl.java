package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.auth.*;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.BusinessExcepion;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.MailIntegration;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.model.redis.UserActivationToken;
import com.client.productionreview.model.redis.UserRecoveryCode;
import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.repositories.redis.UserActivationTokenRepository;
import com.client.productionreview.repositories.redis.UserRecoveryCodeRepository;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.UserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    static final int MAX_RECOVERY_CODE_ATTEMPTS = 5;

    private static final String INVALID_CREDENTIALS = "Senha ou usuário inválido";

    private final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final MailIntegration mailIntegration;
    private final UserRecoveryCodeRepository userRecoveryCodeRepository;
    private final UserActivationTokenRepository userActivationTokenRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final long recoveryCodeTimeoutMinutes;
    private final List<String> allowedOrigins;
    private final String frontendUrl;
    private final DomainEventPublisher eventPublisher;

    public UserDetailsServiceImpl(UserRepository userRepository, MailIntegration mailIntegration, PasswordEncoder passwordEncoder, JwtProvider jwtProvider, UserRecoveryCodeRepository userRecoveryCodeRepository,
                                  UserActivationTokenRepository userActivationTokenRepository,
                                  RoleRepository roleRepository,
                                  @Value("${webservices.productionreview.redis.recoverycode.timeout}") long recoveryCodeTimeoutMinutes,
                                  @Value("${app.allowed-origins}") List<String> allowedOrigins,
                                  @Value("${app.frontend-url}") String frontendUrl,
                                  DomainEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.mailIntegration = mailIntegration;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.userRecoveryCodeRepository = userRecoveryCodeRepository;
        this.userActivationTokenRepository = userActivationTokenRepository;
        this.roleRepository = roleRepository;
        this.recoveryCodeTimeoutMinutes = recoveryCodeTimeoutMinutes;
        this.allowedOrigins = allowedOrigins;
        this.frontendUrl = frontendUrl;
    }


    @Override
    public AutoSignInDTOResponse loadUserByUsernameAndPass(AuthSignInDTORequest authSignInDTORequest, String origin) {
        User user = userRepository.findByUsernameOrEmail(authSignInDTORequest.getUsername(), authSignInDTORequest.getUsername())
                .orElseThrow(() -> new GlobalException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

        // a senha é conferida antes do status da conta para não revelar se o usuário existe/está ativo
        var passwordMatches = passwordEncoder.matches(authSignInDTORequest.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new GlobalException(INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        if (!Boolean.TRUE.equals(user.getActive())) {
            activateAccount(origin, user);
            throw new GlobalException("Usuário não ativado",  HttpStatus.FORBIDDEN);
        }

        AutoSignInDTOResponse response = AutoSignInDTOResponse.builder()
                .token(jwtProvider.generateToken(user.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .build();

        eventPublisher.publish(EventType.USER_LOGGED_IN, user.getId(), user.getName() + " entrou no sistema", user);
        return response;
    }

    @Override
    public void signUp(AuthSignUpDTORequest authSignUpDTORequest, String origin) {

        Optional<User> exists = userRepository.findByUsernameOrEmail(authSignUpDTORequest.getUsername(), authSignUpDTORequest.getEmail());

        if (exists.isPresent()) {
            throw new BusinessExcepion("Usuário ou email já cadastrado");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Role não encontrada"));

        User user = new User();
        user.setUsername(authSignUpDTORequest.getUsername());
        user.setName(authSignUpDTORequest.getName());
        user.setEmail(authSignUpDTORequest.getEmail());
        user.setPassword(passwordEncoder.encode(authSignUpDTORequest.getPassword()));
        user.setActive(false);
        user.setRoles(new ArrayList<>(List.of(userRole)));

        User saved = userRepository.save(user);

        activateAccount(origin, user);

        User actor = saved != null ? saved : user;
        eventPublisher.publish(EventType.USER_SIGNED_UP, actor.getId(), actor.getName() + " se cadastrou", actor);
    }

    private void activateAccount(String origin, User user) {
        String subject = "Confirme seu e-mail para ativar sua conta";
        String token = UUID.randomUUID().toString();

        String confirmationLink = resolveBaseUrl(origin) + "/activate-account/" + token;

        String body = "Olá " + user.getUsername() + ",\n\n"
                + "Obrigado por se registrar no nosso sistema! Para ativar sua conta, por favor, confirme seu e-mail clicando no link abaixo:\n\n"
                + confirmationLink + "\n\n"
                + "Se você não se registrou em nosso sistema, por favor, ignore este e-mail.\n\n"
                + "Atenciosamente,\n"
                + "production-review";

        userActivationTokenRepository.save(UserActivationToken.builder().token(token).email(user.getEmail()).build());

        mailIntegration.send(user.getEmail(), body, subject);
    }

    /**
     * O header Origin é controlado pelo cliente; só o usamos no link do e-mail
     * quando ele está na lista de origens permitidas.
     */
    private String resolveBaseUrl(String origin) {
        if (origin != null && allowedOrigins.contains(origin)) {
            return origin;
        }
        return frontendUrl;
    }

    @Override
    public void sendRecoveryCode(ForgotPasswordRequest email) {
        if (userRepository.findByEmail(email.getEmail()).isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        UserRecoveryCode userRecoveryCode = userRecoveryCodeRepository.findByEmail(email.getEmail())
                .orElseGet(() -> UserRecoveryCode.builder().email(email.getEmail()).build());

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        userRecoveryCode.setCode(code);
        userRecoveryCode.setCreatedDate(LocalDateTime.now());
        userRecoveryCode.setFailedAttempts(0);
        userRecoveryCode.setTtl(recoveryCodeTimeoutMinutes * 60);

        userRecoveryCodeRepository.save(userRecoveryCode);

        mailIntegration.send(email.getEmail(), "Seu código de recuperação é: " + code, "Código de recuperação");
    }


    @Override
    public boolean recoveryCodeIsValid(String recoveryCode, String email) {
        UserRecoveryCode userRecoveryCode = userRecoveryCodeRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Código de recuperação inválido"));

        LocalDateTime createdDate = Objects.requireNonNullElse(userRecoveryCode.getCreatedDate(), LocalDateTime.MIN);
        boolean expired = !LocalDateTime.now().isBefore(createdDate.plusMinutes(recoveryCodeTimeoutMinutes));

        if (expired) {
            userRecoveryCodeRepository.delete(userRecoveryCode);
            return false;
        }

        if (recoveryCode != null && recoveryCode.equals(userRecoveryCode.getCode())) {
            return true;
        }

        // limita tentativas para impedir força bruta no código numérico
        int attempts = Objects.requireNonNullElse(userRecoveryCode.getFailedAttempts(), 0) + 1;
        if (attempts >= MAX_RECOVERY_CODE_ATTEMPTS) {
            userRecoveryCodeRepository.delete(userRecoveryCode);
        } else {
            userRecoveryCode.setFailedAttempts(attempts);
            userRecoveryCodeRepository.save(userRecoveryCode);
        }
        return false;
    }

    @Override
    public void updatePasswordByRecoveryCode(AuthUpdatePasswordDTORequest userDetailsDto) {

        if (!recoveryCodeIsValid(userDetailsDto.getRecoveryCode(), userDetailsDto.getEmail())) {
            throw new BadRequestException("Código de recuperação inválido");
        }

        User userCredentials = userRepository.findByEmail(userDetailsDto.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        userCredentials.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));

        userRepository.save(userCredentials);

        // o código só pode ser usado uma vez
        userRecoveryCodeRepository.findByEmail(userDetailsDto.getEmail())
                .ifPresent(userRecoveryCodeRepository::delete);
    }

    @Override
    public void activeUserByRecoveryCode(String recoveryCode) {

        UserActivationToken activationToken = userActivationTokenRepository.findById(recoveryCode)
                .orElseThrow(() -> new NotFoundException("Código de ativação inválido"));

        User userCredentials = userRepository.findByEmail(activationToken.getEmail())
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));

        userCredentials.setActive(true);

        userRepository.save(userCredentials);

        userActivationTokenRepository.delete(activationToken);

        eventPublisher.publish(EventType.USER_ACTIVATED, userCredentials.getId(),
                userCredentials.getName() + " ativou a conta", userCredentials);
    }

    @Override
    public AutoSignInDTOResponse refreshToken(HttpServletRequest request) {

        var token = jwtProvider.getRefreshTokenFromCookie(request);

        Long idUser = jwtProvider.getUserIdFromRefreshToken(token);

        // usuário desativado não renova a sessão
        boolean valid = idUser != null && userRepository.findById(idUser)
                .map(found -> Boolean.TRUE.equals(found.getActive()))
                .orElse(false);
        if (!valid) {
            throw new GlobalException("Token inválido", HttpStatus.UNAUTHORIZED);
        }

        return AutoSignInDTOResponse.builder()
                .token(jwtProvider.generateToken(idUser))
                .refreshToken(jwtProvider.generateRefreshToken(idUser))
                .build();
    }

    @Override
    public Map<String, ResponseCookie> logout() {
        Map<String, ResponseCookie> response = new HashMap<>();
        response.put("token", jwtProvider.cleanToken());
        response.put("refreshToken", jwtProvider.cleanRefreshToken());
        return response;
    }


}
