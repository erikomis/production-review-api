package com.client.productionreview.dtos.admin;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminRequestDTO {

    @NotNull(message = "Admin is required")
    private Boolean admin;
}
