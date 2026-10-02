package com.client.productionreview.repository;

import com.client.productionreview.model.jpa.Role;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.RoleRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles(profiles = "test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        Role admin = new Role();
        admin.setName("ADMIN");
        admin = roleRepository.save(admin);
        Role user = new Role();
        user.setName("USER");
        user = roleRepository.save(user);

        userRepository.save(User.builder().name("Administrador").username("admin").email("admin@local.dev")
                .password("p").active(true).roles(List.of(admin, user)).build());
        userRepository.save(User.builder().name("Maria Souza").username("maria").email("maria@mail.com")
                .password("p").active(true).roles(List.of(user)).build());
        userRepository.save(User.builder().name("João").username("joao").email("joao@mail.com")
                .password("p").active(false).roles(List.of(user)).build());
    }

    private List<String> search(String pattern, Boolean active, Boolean isAdmin) {
        return userRepository.search(pattern, active, isAdmin, PageRequest.of(0, 10, Sort.by("username"))).getContent()
                .stream().map(User::getUsername).toList();
    }

    @Test
    void search_byNameUsernameOrEmail() {
        assertEquals(List.of("maria"), search("%souza%", null, null));
        assertEquals(List.of("joao"), search("%joao@%", null, null));
        assertEquals(List.of("admin", "joao", "maria"), search(null, null, null));
    }

    @Test
    void search_byActiveAndRole() {
        assertEquals(List.of("joao"), search(null, false, null));
        assertEquals(List.of("admin"), search(null, null, true));
        // role=USER no painel = quem não é admin
        assertEquals(List.of("joao", "maria"), search(null, null, false));
        assertEquals(List.of("maria"), search(null, true, false));
    }

    @Test
    void createdAt_isFilledOnInsert() {
        assertEquals(3, userRepository.findCreatedSince(Instant.now().minus(5, ChronoUnit.MINUTES)).size());
        assertTrue(userRepository.findCreatedSince(Instant.now().plus(1, ChronoUnit.DAYS)).isEmpty());
    }
}
