package com.client.productionreview.controller;

import com.client.productionreview.dtos.auth.AutoSignInDTOResponse;
import com.client.productionreview.service.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles(profiles = "test")
class AuthenticationControllerTest {

    private static final String POLICY = "password: A senha deve ter de 8 a 72 caracteres, com letras e números";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    private static String signUp(String password) {
        return "{\"name\":\"Maria\",\"email\":\"maria@mail.com\",\"username\":\"maria\",\"password\":\"" + password + "\"}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc12", "somenteletras", "1234567890", "a1"})
    void signUp_weakPassword_returns400WithPolicyMessage(String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(signUp(password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(POLICY));

        verifyNoInteractions(userDetailsService);
    }

    @Test
    void signUp_passwordTooLong_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON)
                        .content(signUp("a1" + "x".repeat(71))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(POLICY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"senha123", "Ação2026", "Usuario@123"})
    void signUp_validPassword_returns201(String password) throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(signUp(password)))
                .andExpect(status().isCreated());

        verify(userDetailsService).signUp(any(), any());
    }

    @Test
    void resetPassword_appliesThePolicy() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/recovery-code/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@mail.com\",\"password\":\"123\",\"recoveryCode\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(POLICY));

        mockMvc.perform(patch("/api/v1/auth/recovery-code/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"maria@mail.com\",\"password\":\"novaSenha1\",\"recoveryCode\":\"123456\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void signIn_doesNotValidatePasswordFormat() throws Exception {
        when(userDetailsService.loadUserByUsernameAndPass(any(), any())).thenReturn(AutoSignInDTOResponse.builder()
                .token(ResponseCookie.from("token", "a").build())
                .refreshToken(ResponseCookie.from("refresh_token", "r").build())
                .build());

        // contas antigas com senha fora da política continuam entrando
        mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"antigo\",\"password\":\"abc\"}"))
                .andExpect(status().isOk());
    }
}
