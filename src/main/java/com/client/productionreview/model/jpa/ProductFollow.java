package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

/** Usuário que segue um produto (recebe aviso de novas avaliações). */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_follow")
@IdClass(ProductFollow.Key.class)
public class ProductFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_at")
    private Instant createdAt;

    public ProductFollow(Long productId, Long userId) {
        this.productId = productId;
        this.userId = userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Key implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long productId;
        private Long userId;
    }
}
