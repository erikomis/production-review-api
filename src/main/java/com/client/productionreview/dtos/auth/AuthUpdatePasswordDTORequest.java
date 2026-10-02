package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUpdatePasswordDTORequest {
    @Email(message = "Email inválido")
    @NotBlank(message = "Atributo email é obrigatório")
    private String email;

    @NotNull(message = "Atributo password é obrigatório")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;

    @NotBlank(message = "Atributo recoveryCode é obrigatório")
    private  String recoveryCode;
}
