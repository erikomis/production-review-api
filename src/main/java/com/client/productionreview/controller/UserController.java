package com.client.productionreview.controller;

import com.client.productionreview.controller.mapper.UserMapper;
import com.client.productionreview.dtos.UserResponseDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.service.impl.UserServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping(value = "/api/v1/user")
public class UserController {



    private final UserServiceImpl userService;

    private final UserMapper userMapper;

    public UserController(UserServiceImpl userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping(value = "/me")
    @SecurityRequirement(name = "jwt_auth")
    public UserResponseDTO me (
            final Principal request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            User user = (User) authentication.getPrincipal();
            Long userId = user.getId();
            User userDetails = userService.me(userId);
            return userMapper.toDTO(userDetails);
        } else {
            throw new NotFoundException("Usuário não autenticado");
        }
    }
}
