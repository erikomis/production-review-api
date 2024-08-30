package com.client.productionreview.controller.mapper;

import com.client.productionreview.dtos.PermissionDTO;
import com.client.productionreview.dtos.RoleDTO;
import com.client.productionreview.dtos.UserResponseDTO;

import com.client.productionreview.model.jpa.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {


    public UserResponseDTO toDTO(User user) {
        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(user.getId());
        userResponseDTO.setName(user.getName());
        userResponseDTO.setEmail(user.getEmail());
        userResponseDTO.setUsername(user.getUsername());
        userResponseDTO.setActive(user.getActive());
        userResponseDTO.setRoles(user.getRoles().stream()
                .map(role -> {
                    RoleDTO roleDTO = new RoleDTO();
                    roleDTO.setId(role.getId());
                    roleDTO.setName(role.getName());
                    roleDTO.setPermissions(role.getPermissions().stream()
                            .map(permission -> {
                                PermissionDTO permissionDTO = new PermissionDTO();
                                permissionDTO.setId(permission.getId());
                                permissionDTO.setName(permission.getName());
                                return permissionDTO;
                            }).toList());
                    return roleDTO;
                }).toList());

        return userResponseDTO;
    }
}
