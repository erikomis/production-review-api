package com.client.productionreview.security;

import com.client.productionreview.provider.JwtProvider;
import com.client.productionreview.repositories.jpa.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class AuthenticationFilter  extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;

    public AuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        var token = jwtProvider.getTokenFromCookie(request);
        if (token != null) {
            authByToken(token);
        }
        filterChain.doFilter(request, response);

    }

    /**
     * Token inválido ou usuário inexistente não lança exceção: a requisição segue
     * sem autenticação e o Spring Security responde 401 se a rota exigir login.
     */
    private void authByToken(String token) {
        Long userId = jwtProvider.getUserId(token);
        if (userId == null) {
            return;
        }

        userRepository.findById(userId).ifPresent(user -> {
            var userAuth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(userAuth);
        });
    }

}
