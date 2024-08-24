package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUpdatePasswordDTORequest {
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Atributo password é obrigatório")
    private String password;

    private  String recoveryCode;
}
