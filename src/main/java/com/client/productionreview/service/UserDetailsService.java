package com.client.productionreview.service;

import com.client.productionreview.dtos.auth.*;

public interface UserDetailsService {



    AutoSignInDTOResponse loadUserByUsernameAndPass(AuthSignInDTORequest authSignInDTORequest);

    void signUp(AuthSignUpDTORequest authSignUpDTORequest);

    void sendRecoveryCode(String email);

    boolean recoveryCodeIsValid(String recoveryCode, String email);

    void updatePasswordByRecoveryCode(AuthUpdatePasswordDTORequest userDetailsDto);


    void activeUserByRecoveryCode(String recoveryCode);
}



