package com.client.productionreview.dtos.notification;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesDTO {

    @NotNull(message = "emailNotifications é obrigatório")
    private Boolean emailNotifications;
}
