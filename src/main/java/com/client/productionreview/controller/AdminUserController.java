package com.client.productionreview.controller;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.dtos.admin.UserActiveRequestDTO;
import com.client.productionreview.dtos.admin.UserAdminRequestDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.AdminUserService;
import com.client.productionreview.utils.PaginationUtils;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@SecurityRequirement(name = "jwt_auth")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public Page<AdminUserDTO> listUsers(@RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "size", required = false) Integer size,
                                        @RequestParam(value = "search", required = false) String search,
                                        @RequestParam(value = "role", required = false) String role,
                                        @RequestParam(value = "active", required = false) Boolean active) {
        Pageable base = PaginationUtils.createPageable(page, size, null, null);
        Pageable pageable = PageRequest.of(base.getPageNumber(), base.getPageSize(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return adminUserService.listUsers(search, role, active, pageable);
    }

    @PatchMapping("/{id}/admin")
    public AdminUserDTO setAdmin(@PathVariable("id") Long id, @Valid @RequestBody UserAdminRequestDTO request,
                                 @AuthenticationPrincipal User currentUser) {
        return adminUserService.setAdmin(id, request.getAdmin(), currentUser);
    }

    @PatchMapping("/{id}/active")
    public AdminUserDTO setActive(@PathVariable("id") Long id, @Valid @RequestBody UserActiveRequestDTO request,
                                  @AuthenticationPrincipal User currentUser) {
        return adminUserService.setActive(id, request.getActive(), currentUser);
    }
}
