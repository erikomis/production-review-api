package com.client.productionreview.security;


import com.client.productionreview.exception.UnauthorizedHandler;
import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;
    private final UnauthorizedHandler unauthorizedHandler;
    private final AccessDeniedHandler accessDeniedHandler;


    private static final String[] PERMIT_ALL_LIST = {
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/v2/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**",
            // sem isso, um 404 de usuário autenticado vira 401 ao ser encaminhado para /error
            "/error",
            "/api/v1/auth/sign-up",
            "/api/v1/auth/sign-in",
            "/api/v1/auth/logout",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/send-recovery-code/send",
            "/api/v1/auth/recovery-code",
            "/api/v1/auth/recovery-code/password",
            "/api/v1/auth/activate/**"
    };

    private static final String[] PUBLIC_GET_LIST = {
            "/api/v1/category/**",
            "/api/v1/sub-categorie/**",
            "/api/v1/production/**",
            "/api/v1/review/**"
    };


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                // usa a configuração de CORS do WebMvcConfig, inclusive no preflight (OPTIONS)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(unauthorizedHandler)
                                .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PERMIT_ALL_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_LIST).permitAll()
                        .anyRequest().authenticated()
                ).addFilterBefore(new AuthenticationFilter(jwtProvider, userRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
