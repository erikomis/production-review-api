package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Atributo password é obrigatório")
    @Size(min = 3 , max = 20, message = "A senha deve ter entre 3 e 20 caracteres")
    private String password;

    @NotBlank(message = "Atributo recoveryCode é obrigatório")
    private  String recoveryCode;
}
