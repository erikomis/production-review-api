package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.AdminUserService;
import com.client.productionreview.service.DomainEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    static final String ADMIN = "ADMIN";
    static final String USER = "USER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ReviewRepository reviewRepository;
    private final DomainEventPublisher eventPublisher;

    public AdminUserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                                ReviewRepository reviewRepository, DomainEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Page<AdminUserDTO> listUsers(String search, String role, Boolean active, Pageable pageable) {
        Boolean admin = null;
        if (role != null && !role.isBlank()) {
            String normalized = role.trim().toUpperCase();
            if (!ADMIN.equals(normalized) && !USER.equals(normalized)) {
                throw new BadRequestException("Role inválida: " + role);
            }
            admin = ADMIN.equals(normalized);
        }
        String pattern = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";

        Page<User> users = userRepository.search(pattern, active, admin, pageable);

        Map<Long, Long> counts = new HashMap<>();
        if (!users.isEmpty()) {
            reviewRepository.countByUsers(users.getContent().stream().map(User::getId).toList())
                    .forEach(count -> counts.put(count.getUserId(), count.getTotal()));
        }

        List<AdminUserDTO> content = users.getContent().stream()
                .map(user -> toDTO(user, counts.getOrDefault(user.getId(), 0L)))
                .toList();
        return new PageImpl<>(content, pageable, users.getTotalElements());
    }

    @Override
    public AdminUserDTO setAdmin(Long userId, boolean admin, User currentUser) {
        User user = findUser(userId);

        if (!admin && currentUser != null && Objects.equals(currentUser.getId(), userId)) {
            throw new BadRequestException("Você não pode remover a sua própria permissão de administrador");
        }

        List<Role> roles = new ArrayList<>(user.getRoles() == null ? List.of() : user.getRoles());
        boolean isAdmin = roles.stream().anyMatch(role -> ADMIN.equals(role.getName()));

        if (admin && !isAdmin) {
            roles.add(findRole(ADMIN));
        } else if (!admin && isAdmin) {
            roles.removeIf(role -> ADMIN.equals(role.getName()));
        }
        // todo usuário continua com a role USER
        if (roles.stream().noneMatch(role -> USER.equals(role.getName()))) {
            roles.add(findRole(USER));
        }
        user.setRoles(roles);
        User saved = userRepository.save(user);

        if (admin != isAdmin) {
            eventPublisher.publish(EventType.USER_ROLE_CHANGED, userId, admin
                    ? nameOf(saved) + " recebeu permissão de administrador"
                    : nameOf(saved) + " perdeu a permissão de administrador");
        }
        return toDTO(saved, reviewCount(userId));
    }

    @Override
    public AdminUserDTO setActive(Long userId, boolean active, User currentUser) {
        User user = findUser(userId);

        if (!active && currentUser != null && Objects.equals(currentUser.getId(), userId)) {
            throw new BadRequestException("Você não pode desativar a sua própria conta");
        }

        boolean changed = !Objects.equals(user.getActive(), active);
        user.setActive(active);
        User saved = userRepository.save(user);

        if (changed) {
            eventPublisher.publish(EventType.USER_STATUS_CHANGED, userId,
                    nameOf(saved) + (active ? " foi ativado" : " foi desativado"));
        }
        return toDTO(saved, reviewCount(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Role findRole(String name) {
        return roleRepository.findByName(name).orElseThrow(() -> new NotFoundException("Role não encontrada"));
    }

    private long reviewCount(Long userId) {
        return reviewRepository.countByUsers(List.of(userId)).stream()
                .findFirst().map(ReviewRepository.UserCount::getTotal).orElse(0L);
    }

    private static String nameOf(User user) {
        return user.getName() == null ? "Usuário " + user.getId() : user.getName().trim();
    }

    static AdminUserDTO toDTO(User user, long reviewsCount) {
        List<String> roles = user.getRoles() == null ? List.of() : user.getRoles().stream()
                .map(Role::getName)
                .distinct()
                .sorted()
                .toList();
        return AdminUserDTO.builder()
                .id(user.getId())
                .name(user.getName() == null ? null : user.getName().trim())
                .username(user.getUsername() == null ? null : user.getUsername().trim())
                .email(user.getEmail() == null ? null : user.getEmail().trim())
                .active(user.getActive())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .reviewsCount(reviewsCount)
                .build();
    }
}
