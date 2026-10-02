package com.client.productionreview.security;

import com.client.productionreview.model.jpa.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Acesso ao usuário autenticado da requisição atual (vazio em rotas anônimas). */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<User> get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public static Long id() {
        return get().map(User::getId).orElse(null);
    }

    public static boolean isAdmin(User user) {
        return user != null && user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getName()));
    }
}
