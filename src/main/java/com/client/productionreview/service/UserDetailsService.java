package com.client.productionreview.service;

import com.client.productionreview.dtos.auth.*;
import com.client.productionreview.model.jpa.User;
import jakarta.servlet.http.HttpServletRequest;


public interface UserDetailsService {



    AutoSignInDTOResponse loadUserByUsernameAndPass(AuthSignInDTORequest authSignInDTORequest);

    void signUp(AuthSignUpDTORequest authSignUpDTORequest, String origin);

    void sendRecoveryCode(String email);

    boolean recoveryCodeIsValid(String recoveryCode, String email);

    void updatePasswordByRecoveryCode(AuthUpdatePasswordDTORequest userDetailsDto);

    void activeUserByRecoveryCode(String recoveryCode);

    AutoSignInDTOResponse refreshToken(HttpServletRequest request);

    void logout(HttpServletRequest request);
}



