package com.client.productionreview.controller;


import com.client.productionreview.dtos.auth.*;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.UserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpHeaders.SET_COOKIE;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(value = "api/v1/auth")
public class AuthenticationController {


    private final UserDetailsService userService;

    public AuthenticationController(UserDetailsService userService) {
        this.userService = userService;
    }


    @PostMapping("/sign-in")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> signIn(@Valid @RequestBody AuthSignInDTORequest authSignInDTORequest) {
        final var responseCookie = userService.loadUserByUsernameAndPass(authSignInDTORequest);
        return ok().
                header(SET_COOKIE, responseCookie.getToken().toString()).
                header(SET_COOKIE, responseCookie.getRefreshToken().toString())
                .body(null);

    }

    @PostMapping("/sign-up")
    @ResponseStatus(HttpStatus.CREATED)
    public void signUp(@Valid @RequestBody AuthSignUpDTORequest authSignUpDTORequest) {
        userService.signUp(authSignUpDTORequest);
    }


    @GetMapping("/activate/{token}")
    @ResponseStatus(HttpStatus.OK)
    public void activate(@PathVariable String token) {
        userService.activeUserByRecoveryCode(token);
    }

    @PostMapping("/send-recovery-code/send")
    public ResponseEntity<?> sendRecoveryCode(@Valid @RequestBody String email) {
        userService.sendRecoveryCode(email);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @GetMapping("/recovery-code")
    public ResponseEntity<?> recoveryCodeIsValid(@RequestParam("recoveryCode") String recoveryCode, @RequestParam("email") String email) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.recoveryCodeIsValid(recoveryCode, email));
    }


    @PatchMapping("/recovery-code/password")
    public ResponseEntity<?> updatePasswordByRecoveryCode(@Valid @RequestBody AuthUpdatePasswordDTORequest userDetailsDto) {
        userService.updatePasswordByRecoveryCode(userDetailsDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
    }

    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> refreshToken(HttpServletRequest request ) {

        final var responseCookie = userService.refreshToken(request);
        return ok().
                header(SET_COOKIE, responseCookie.getToken().toString()).
                header(SET_COOKIE, responseCookie.getRefreshToken().toString())
                .body(null);

    }

}
