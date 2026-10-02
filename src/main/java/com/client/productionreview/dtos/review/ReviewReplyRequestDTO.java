package com.client.productionreview.dtos.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewReplyRequestDTO {

    @NotBlank(message = "O texto da resposta é obrigatório")
    @Size(max = 1000, message = "A resposta deve ter no máximo 1000 caracteres")
    private String text;
}
