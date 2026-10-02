package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByEmail(String username);

    /**
     * Listagem do painel. {@code pattern} já vem em minúsculas com curingas; {@code admin} filtra quem
     * tem (true) ou não tem (false) a role ADMIN; parâmetros nulos não filtram.
     */
    @Query(value = "SELECT u FROM User u WHERE "
            + "(:pattern IS NULL OR LOWER(u.name) LIKE :pattern OR LOWER(u.username) LIKE :pattern OR LOWER(u.email) LIKE :pattern) "
            + "AND (:active IS NULL OR u.active = :active) "
            + "AND (:admin IS NULL OR (:admin = TRUE AND EXISTS (SELECT 1 FROM User a JOIN a.roles ar WHERE a.id = u.id AND ar.name = 'ADMIN')) "
            + "OR (:admin = FALSE AND NOT EXISTS (SELECT 1 FROM User a JOIN a.roles ar WHERE a.id = u.id AND ar.name = 'ADMIN')))",
            countQuery = "SELECT COUNT(u) FROM User u WHERE "
                    + "(:pattern IS NULL OR LOWER(u.name) LIKE :pattern OR LOWER(u.username) LIKE :pattern OR LOWER(u.email) LIKE :pattern) "
                    + "AND (:active IS NULL OR u.active = :active) "
                    + "AND (:admin IS NULL OR (:admin = TRUE AND EXISTS (SELECT 1 FROM User a JOIN a.roles ar WHERE a.id = u.id AND ar.name = 'ADMIN')) "
                    + "OR (:admin = FALSE AND NOT EXISTS (SELECT 1 FROM User a JOIN a.roles ar WHERE a.id = u.id AND ar.name = 'ADMIN')))")
    Page<User> search(@Param("pattern") String pattern, @Param("active") Boolean active, @Param("admin") Boolean admin,
                      Pageable pageable);

    @Query("SELECT u.createdAt FROM User u WHERE u.createdAt >= :from")
    List<Instant> findCreatedSince(@Param("from") Instant from);
}
