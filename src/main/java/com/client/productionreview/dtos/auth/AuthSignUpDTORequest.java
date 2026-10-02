package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuthSignUpDTORequest {


    @NotBlank(message = "O campo name é obrigatório")
    private String name;

    @Email(message = "O campo email deve ser um email válido")
    @NotBlank(message = "O campo email é obrigatório")
    private String email;

   @NotBlank(message = "O campo username é obrigatório")
    private String username;

    @NotNull(message = "O campo password é obrigatório")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String password;
}
