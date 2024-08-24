package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class AuthSignInDTORequest {

    @NotBlank(message = "O campo username é obrigatório")
    private String username;

    @NotBlank(message = "O campo password é obrigatório")
    private String password;
}
