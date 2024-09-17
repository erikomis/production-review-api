package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.auth.*;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.MailIntegration;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.model.redis.UserRecoveryCode;
import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.repositories.redis.UserRecoveryCodeRepository;
import com.client.productionreview.service.UserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {


    private final UserRepository userRepository;
    private final MailIntegration mailIntegration;
    private final UserRecoveryCodeRepository userRecoveryCodeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    @Value("${webservices.productionreview.redis.recoverycode.timeout}")
    private String recoveryCodeTimeout;

    public UserDetailsServiceImpl(UserRepository userRepository, MailIntegration mailIntegration, PasswordEncoder passwordEncoder, JwtProvider jwtProvider, UserRecoveryCodeRepository userRecoveryCodeRepository,
                                  RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.mailIntegration = mailIntegration;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.userRecoveryCodeRepository = userRecoveryCodeRepository;
        this.roleRepository = roleRepository;

    }


    @Override
    public AutoSignInDTOResponse loadUserByUsernameAndPass(AuthSignInDTORequest authSignInDTORequest, String origin) {
        Optional<User> exists = userRepository.findByUsernameOrEmail(authSignInDTORequest.getUsername(), authSignInDTORequest.getUsername());

        if (exists.isEmpty()) {
            throw new NotFoundException("Senha ou usuário inválido");
        }

        User user = exists.get();

        if(user.getActive()){
            activateAccount(origin, user);
            throw new GlobalException("Usuário não ativado",  HttpStatus.FORBIDDEN);
        }

        var passwordMatches = passwordEncoder.matches(authSignInDTORequest.getPassword(), user.getPassword());

        if (!passwordMatches) {
            throw new NotFoundException("Senha ou usuário inválido");
        }


        return AutoSignInDTOResponse.builder()
                .token(jwtProvider.generateToken(user.getId()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getId()))
                .build();

    }

    @Override
    public void signUp(AuthSignUpDTORequest authSignUpDTORequest, String origin) {

        Optional<User> exists = userRepository.findByUsernameOrEmail(authSignUpDTORequest.getUsername(), authSignUpDTORequest.getEmail());

        if (exists.isPresent()) {
            throw new NotFoundException("Usuário ou email já cadastrado");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new NotFoundException("Role não encontrada"));



        User user = new User();
        user.setUsername(authSignUpDTORequest.getUsername());
        user.setName(authSignUpDTORequest.getName());
        user.setEmail(authSignUpDTORequest.getEmail());
        user.setPassword(passwordEncoder.encode(authSignUpDTORequest.getPassword()));
        user.setActive(false);
        user.setRoles(Collections.singletonList(userRole));



        userRepository.save(user);


        activateAccount(origin, user);


    }

    private void activateAccount(String origin, User user) {
        String subject = "Confirme seu e-mail para ativar sua conta";
        String token = UUID.randomUUID().toString();

        String confirmationLink = origin + "/activate-account/" + token;

        String body = "Olá " + user.getUsername() + ",\n\n"
                + "Obrigado por se registrar no nosso sistema! Para ativar sua conta, por favor, confirme seu e-mail clicando no link abaixo:\n\n"
                + confirmationLink + "\n\n"
                + "Se você não se registrou em nosso sistema, por favor, ignore este e-mail.\n\n"
                + "Atenciosamente,\n"
                + "production-review";


        userRecoveryCodeRepository.save(UserRecoveryCode.builder().email(user.getEmail()).code(token).build());

        mailIntegration.send(user.getEmail(), body, subject);
    }

    @Override
    public void sendRecoveryCode(ForgotPasswordRequest email) {
        UserRecoveryCode userRecoveryCode;
        String code = String.format("%04d", new Random().nextInt(10000));

        var userRecoveryCodeOpt = userRecoveryCodeRepository.findByEmail(email.getEmail());

        if (userRecoveryCodeOpt.isEmpty()) {
            var userDetailOpt = userRepository.findByEmail(email.getEmail());

            if (userDetailOpt.isEmpty()) {
                throw new NotFoundException("Usuário não encontrado");
            }

            userRecoveryCode = new UserRecoveryCode();
            userRecoveryCode.setEmail(email.getEmail());

        } else {
            userRecoveryCode = userRecoveryCodeOpt.get();

        }
        userRecoveryCode.setCode(code);
        userRecoveryCode.setCreatedDate(LocalDateTime.now());

        userRecoveryCodeRepository.save(userRecoveryCode);

        mailIntegration.send(email.getEmail(), "Seu código de recuperação é: " + code, "Código de recuperação");


    }


    @Override
    public boolean recoveryCodeIsValid(String recoveryCode, String email) {
        var userRecoveryCodeOpt = userRecoveryCodeRepository.findByEmail(email);

        if (userRecoveryCodeOpt.isEmpty()) {
            throw new NotFoundException("Código de recuperação inválido");
        }

        UserRecoveryCode userRecoveryCode = userRecoveryCodeOpt.get();


        LocalDateTime timeout = userRecoveryCode.getCreatedDate().plusMinutes(Long.parseLong(recoveryCodeTimeout));

        LocalDateTime now = LocalDateTime.now();


        return recoveryCode.equals(userRecoveryCode.getCode()) && now.isBefore(timeout);
    }

    @Override
    public void updatePasswordByRecoveryCode(AuthUpdatePasswordDTORequest userDetailsDto) {

        if (!recoveryCodeIsValid(userDetailsDto.getRecoveryCode(), userDetailsDto.getEmail())) {
            throw new BadRequestException("Código de recuperação inválido");
        }

        Optional<User> userDetailOpt = userRepository.findByEmail(userDetailsDto.getEmail());


        if (userDetailOpt.isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        User userCredentials = userDetailOpt.get();

        userCredentials.setPassword(passwordEncoder.encode(userDetailsDto.getPassword()));


        userRepository.save(userCredentials);

    }

    @Override
    public void activeUserByRecoveryCode(String recoveryCode) {

        var userRecoveryCodeOpt = userRecoveryCodeRepository.findByCode(recoveryCode);

        if (userRecoveryCodeOpt.isEmpty()) {
            throw new NotFoundException("Código de recuperação inválido");
        }

        UserRecoveryCode userRecoveryCode = userRecoveryCodeOpt.get();

        Optional<User> userDetailOpt = userRepository.findByEmail(userRecoveryCode.getEmail());

        if (userDetailOpt.isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        User userCredentials = userDetailOpt.get();

        userCredentials.setActive(true);

        userRepository.save(userCredentials);

    }

    @Override
    public AutoSignInDTOResponse refreshToken(HttpServletRequest request) {


        var token = jwtProvider.getRefreshTokenFromCookie(request);
        if (token == null) {
            throw new BadRequestException("Token inválido");
        }
        
        var jwt = jwtProvider.isValidRefreshToken(token);

        if (!jwt) {
            throw new BadRequestException("Token inválido");
        }

        var idUser = jwtProvider.getUserIdFromRefreshToken(token);


       jwtProvider.cleanToken();
       jwtProvider.cleanRefreshToken();


        return AutoSignInDTOResponse.builder()
                .token(jwtProvider.generateToken(idUser))
                .refreshToken(jwtProvider.generateRefreshToken(idUser))
                .build();
    }

    @Override
    public Map<String, ResponseCookie> logout() {
        var token =   jwtProvider.cleanToken();
        var  refreshToken =  jwtProvider.cleanRefreshToken();

        Map<String,
                ResponseCookie> response = new HashMap<>();
        response.put("token", token);
        response.put("refreshToken", refreshToken);

        return response;

    }


}
