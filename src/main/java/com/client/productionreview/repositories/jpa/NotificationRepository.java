package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.Notification;
import com.client.productionreview.model.jpa.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Mais recentes primeiro; {@code unreadOnly} = true mostra só as não lidas. */
    @Query(value = "SELECT n FROM Notification n WHERE n.userId = :userId AND (:unreadOnly = FALSE OR n.read = FALSE) "
            + "ORDER BY n.createdAt DESC, n.id DESC",
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND (:unreadOnly = FALSE OR n.read = FALSE)")
    Page<Notification> findForUser(@Param("userId") Long userId, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

    long countByUserIdAndReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    Optional<Notification> findFirstByUserIdAndTypeAndReviewIdAndReadFalseOrderByIdDesc(Long userId, NotificationType type,
                                                                                      Long reviewId);

    @Transactional
    @Modifying
    @Query("UPDATE Notification n SET n.read = TRUE WHERE n.userId = :userId AND n.read = FALSE")
    int markAllRead(@Param("userId") Long userId);
}
