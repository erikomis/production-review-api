package com.client.productionreview.config;

import com.client.productionreview.model.jpa.Permission;
import com.client.productionreview.model.jpa.Role;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;

import java.util.stream.Collectors;
@ToString()
public class RoleGrantedAuthority implements GrantedAuthority {
    private final Role role;

    public RoleGrantedAuthority(Role role) {
        this.role = role;
    }

    @Override
    public String getAuthority() {
        return role.getName();
    }

    public boolean hasPermission(String name) {
        System.out.println("RoleGrantedAuthority: " + role.getPermissions());
        return role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toSet())
                .contains(name);


    }


 }



