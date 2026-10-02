package com.client.productionreview.service;

import com.client.productionreview.dtos.notification.NotificationPreferencesDTO;
import com.client.productionreview.dtos.user.PublicProfileDTO;
import com.client.productionreview.model.jpa.User;

public interface UserService {

     User me(Long id);

     NotificationPreferencesDTO getPreferences(Long userId);

     NotificationPreferencesDTO updatePreferences(Long userId, boolean emailNotifications);

     /** 404 se o usuário não existir ou estiver inativo. */
     PublicProfileDTO getPublicProfile(String username);
}
