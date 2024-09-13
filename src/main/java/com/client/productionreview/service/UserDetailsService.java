package com.client.productionreview.service;

import com.client.productionreview.dtos.auth.*;
import com.client.productionreview.model.jpa.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;

import java.util.Map;


public interface UserDetailsService {



    AutoSignInDTOResponse loadUserByUsernameAndPass(AuthSignInDTORequest authSignInDTORequest);

    void signUp(AuthSignUpDTORequest authSignUpDTORequest, String origin);

    void sendRecoveryCode(ForgotPasswordRequest email);

    boolean recoveryCodeIsValid(String recoveryCode, String email);

    void updatePasswordByRecoveryCode(AuthUpdatePasswordDTORequest userDetailsDto);

    void activeUserByRecoveryCode(String recoveryCode);

    AutoSignInDTOResponse refreshToken(HttpServletRequest request);
     Map<String, ResponseCookie> logout ();
}



