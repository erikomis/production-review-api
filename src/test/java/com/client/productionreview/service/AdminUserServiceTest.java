package com.client.productionreview.service;

import com.client.productionreview.dtos.admin.AdminUserDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AdminUserServiceImpl service;

    private Role adminRole;
    private Role userRole;
    private User me;
    private User maria;

    @BeforeEach
    void setUp() {
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName("ADMIN");
        userRole = new Role();
        userRole.setId(2L);
        userRole.setName("USER");
        me = User.builder().id(1L).name("Administrador").username("admin").active(true)
                .roles(new ArrayList<>(List.of(adminRole, userRole))).build();
        maria = User.builder().id(2L).name("Maria").username("maria").email("maria@mail.com").active(true)
                .roles(new ArrayList<>(List.of(userRole))).build();
    }

    private ReviewRepository.UserCount count(Long userId, Long total) {
        return new ReviewRepository.UserCount() {
            @Override
            public Long getUserId() {
                return userId;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    @Test
    void list_mapsRolesAndReviewCount_andTranslatesFilters() {
        var pageable = PageRequest.of(0, 10);
        when(userRepository.search("%mar%", true, false, pageable)).thenReturn(new PageImpl<>(List.of(maria), pageable, 1));
        when(reviewRepository.countByUsers(List.of(2L))).thenReturn(List.of(count(2L, 3L)));

        Page<AdminUserDTO> page = service.listUsers(" Mar ", "user", true, pageable);

        AdminUserDTO dto = page.getContent().get(0);
        assertEquals("maria", dto.getUsername());
        assertEquals(List.of("USER"), dto.getRoles());
        assertEquals(3L, dto.getReviewsCount());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void list_invalidRole_isBadRequest() {
        assertThrows(BadRequestException.class, () -> service.listUsers(null, "ROOT", null, PageRequest.of(0, 10)));
    }

    @Test
    void grantAdmin_addsRoleAndPublishesEvent() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(maria));
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.save(maria)).thenReturn(maria);

        AdminUserDTO dto = service.setAdmin(2L, true, me);

        assertEquals(List.of("ADMIN", "USER"), dto.getRoles());
        verify(eventPublisher).publish(eq(EventType.USER_ROLE_CHANGED), eq(2L), anyString());
    }

    @Test
    void revokeAdmin_keepsUserRole() {
        User other = User.builder().id(3L).name("Outro").roles(new ArrayList<>(List.of(adminRole))).build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(other));
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(other)).thenReturn(other);

        AdminUserDTO dto = service.setAdmin(3L, false, me);

        assertEquals(List.of("USER"), dto.getRoles());
    }

    @Test
    void revokeOwnAdmin_isBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        assertThrows(BadRequestException.class, () -> service.setAdmin(1L, false, me));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deactivateSelf_isBadRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));

        assertThrows(BadRequestException.class, () -> service.setActive(1L, false, me));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateOther_publishesStatusChange() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(maria));
        when(userRepository.save(maria)).thenReturn(maria);

        AdminUserDTO dto = service.setActive(2L, false, me);

        assertFalse(dto.getActive());
        verify(eventPublisher).publish(eq(EventType.USER_STATUS_CHANGED), eq(2L), contains("desativado"));
    }

    @Test
    void unknownUser_isNotFound() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.setActive(9L, true, me));
    }
}
