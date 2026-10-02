package com.client.productionreview.dtos.auth;

/** Política de senha do cadastro e da redefinição (o login não valida formato). */
public final class PasswordPolicy {

    /** 8 a 72 caracteres (limite do BCrypt), com pelo menos uma letra e um número. */
    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\p{Nd}).{8,72}$";

    public static final String MESSAGE = "A senha deve ter de 8 a 72 caracteres, com letras e números";

    private PasswordPolicy() {
    }
}
