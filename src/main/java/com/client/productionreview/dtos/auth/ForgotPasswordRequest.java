package com.client.productionreview.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ForgotPasswordRequest {
    @Email(message = "O campo email deve ser um email válido")
    @NotBlank(message = "O campo email é obrigatório")
    private String email;
}
