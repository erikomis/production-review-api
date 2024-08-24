package com.client.productionreview.security;


import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;


    public SecurityConfig(
            JwtProvider jwtProvider,
            UserRepository userRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }


    private static final String[] PERMIT_ALL_LIST = {
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/v2/api-docs/**",
            "/swagger-resources/**",
            "/actuator/**"
    };


    @Bean
    SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/v1/category",
                                    "api/v1/category/",
                                    "api/v1/production/list",
                                    "api/v1/sub-categorie" ,
                                    "api/v1/production/list",
                                    "api/v1/review/",
                                    "api/v1/auth/sign-up",
                                    "api/v1/auth/sign-in")
                            .permitAll()
                            .requestMatchers(PERMIT_ALL_LIST).permitAll();
                    auth.anyRequest().authenticated();
                }).addFilterBefore(new AuthenticationFilter(jwtProvider, userRepository), UsernamePasswordAuthenticationFilter.class)

        ;

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
