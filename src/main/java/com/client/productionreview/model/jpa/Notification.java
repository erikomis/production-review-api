package com.client.productionreview.model.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/** Notificação exibida no site (sino do header). {@code link} é um caminho do site. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "notification")
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "link", length = 500)
    private String link;

    /** Review relacionada (usada para agregar as notificações de "útil"). */
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    /** Definido pela aplicação (e renovado quando a notificação agregada é atualizada). */
    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void defaultCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
