package com.client.productionreview.config;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionChecker")
public class PermissionChecker {

    public boolean hasRoleWithPermission(Authentication authentication, String roleName, String permissionName) {
        return authentication.getAuthorities().stream()
                .filter(authority -> authority instanceof RoleGrantedAuthority)
                .map(authority -> (RoleGrantedAuthority) authority)
                .anyMatch(roleAuthority ->
                        roleAuthority.getAuthority().equals(roleName) &&
                                roleAuthority.hasPermission(permissionName));
    }
}