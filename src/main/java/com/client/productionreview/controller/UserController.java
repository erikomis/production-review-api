package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.UserMapper;
import com.client.productionreview.dtos.UserResponseDTO;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {

    private final UserService userService;

    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping(value = "/me")
    @SecurityRequirement(name = "jwt_auth")
    public UserResponseDTO me(@AuthenticationPrincipal User user) {
        return userMapper.toDTO(userService.me(user.getId()));
    }
}
