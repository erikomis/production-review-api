package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.UserMapper;
import com.client.productionreview.dtos.UserResponseDTO;
import com.client.productionreview.dtos.notification.NotificationPreferencesDTO;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.ProductFollowService;
import com.client.productionreview.service.UserService;
import com.client.productionreview.utils.PaginationUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {

    private final UserService userService;

    private final UserMapper userMapper;

    private final ProductFollowService productFollowService;

    public UserController(UserService userService, UserMapper userMapper, ProductFollowService productFollowService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.productFollowService = productFollowService;
    }

    @GetMapping("/me/preferences")
    @SecurityRequirement(name = "jwt_auth")
    public NotificationPreferencesDTO preferences(@AuthenticationPrincipal User user) {
        return userService.getPreferences(user.getId());
    }

    @PatchMapping("/me/preferences")
    @SecurityRequirement(name = "jwt_auth")
    public NotificationPreferencesDTO updatePreferences(@Valid @RequestBody NotificationPreferencesDTO request,
                                                        @AuthenticationPrincipal User user) {
        return userService.updatePreferences(user.getId(), request.getEmailNotifications());
    }

    /** Produtos seguidos pelo usuário logado (ordem alfabética). */
    @GetMapping("/me/following")
    @SecurityRequirement(name = "jwt_auth")
    public Page<ProductSummaryDTO> following(@RequestParam(value = "page", required = false) Integer page,
                                             @RequestParam(value = "size", required = false) Integer size,
                                             @AuthenticationPrincipal User user) {
        return productFollowService.following(user.getId(), PaginationUtils.createPageable(page, size, "name", "ASC"));
    }

    @GetMapping(value = "/me")
    @SecurityRequirement(name = "jwt_auth")
    public UserResponseDTO me(@AuthenticationPrincipal User user) {
        return userMapper.toDTO(userService.me(user.getId()));
    }
}
