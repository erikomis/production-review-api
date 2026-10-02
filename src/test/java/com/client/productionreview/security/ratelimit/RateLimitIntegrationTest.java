package com.client.productionreview.security.ratelimit;

import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.service.UserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Rate limit ligado, passando pela cadeia real do Spring Security (contador em memória no lugar do Redis). */
@SpringBootTest(properties = "app.rate-limit.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RateLimitIntegrationTest {

    @TestConfiguration
    static class Config {
        @Bean
        @Primary
        InMemoryRateLimitStore inMemoryRateLimitStore() {
            return new InMemoryRateLimitStore();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryRateLimitStore store;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        store.clear();
        when(userDetailsService.loadUserByUsernameAndPass(any(), any()))
                .thenThrow(new GlobalException("Senha ou usuário inválido", HttpStatus.UNAUTHORIZED));
    }

    @Test
    void sixWrongLogins_return429WithRetryAfter() throws Exception {
        String body = "{\"username\":\"admin\",\"password\":\"errada\"}";
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.httpStatus").value("TOO_MANY_REQUESTS"))
                .andExpect(jsonPath("$.statusCode").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Muitas tentativas. Tente novamente em ")));
    }
}
