package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

public interface AdminUserService {

    /**
     * {@code role}: ADMIN = usuários com a role ADMIN; USER = usuários sem a role ADMIN.
     * {@code search} busca em nome, username e e-mail.
     */
    Page<AdminUserDTO> listUsers(String search, String role, Boolean active, Pageable pageable);

    AdminUserDTO setAdmin(Long userId, boolean admin, User currentUser);

    AdminUserDTO setActive(Long userId, boolean active, User currentUser);
}
