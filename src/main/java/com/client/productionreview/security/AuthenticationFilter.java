package com.client.productionreview.security;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.User;
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
import java.util.Objects;
import java.util.Optional;

public class AuthenticationFilter  extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private final UserRepository userRepository;

    public AuthenticationFilter(JwtProvider jwtProvider, UserRepository userRepository) {
        this.jwtProvider = jwtProvider;
        this.userRepository = userRepository;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = getBearerToken(request);
        if (Objects.nonNull(token)) {
            authByToken(token);
        }
        filterChain.doFilter(request, response);

    }


    private void authByToken(String token) {
        var tokenOpt = jwtProvider.validateToken(token);

        if (tokenOpt == null) {
             throw new NotFoundException("Token inválido");
        }


        //recuperar id do usuario
        Long userId = jwtProvider.getUserId(token);
        var userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new NotFoundException("Usuário não encontrado");
        }

        User userCredentials = userOpt.get();
        //autenticar no spring

        UsernamePasswordAuthenticationToken userAuth
                = new UsernamePasswordAuthenticationToken(userCredentials, null, userCredentials.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(userAuth);
    }







    private String getBearerToken(HttpServletRequest request) {

        String token = request.getHeader("Authorization");
        if (Objects.isNull(token) || !token.startsWith("Bearer")) {
            return null;
        }

        return token.substring(7);
    }



}
