package com.client.productionreview.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActiveRequestDTO {

    @NotNull(message = "Active is required")
    private Boolean active;
}
