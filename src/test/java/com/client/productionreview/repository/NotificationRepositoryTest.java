package com.client.productionreview.repository;

import com.client.productionreview.model.jpa.Notification;
import com.client.productionreview.model.jpa.NotificationType;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.NotificationRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles(profiles = "test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    private User maria;
    private User ana;
    private Notification old;
    private Notification recentRead;
    private Notification newest;

    @BeforeEach
    void setUp() {
        maria = userRepository.save(User.builder().name("Maria").email("maria@mail.com").username("maria").password("p").active(true).build());
        ana = userRepository.save(User.builder().name("Ana").email("ana@mail.com").username("ana").password("p").active(true).build());

        old = save(maria, NotificationType.REVIEW_HELPFUL, 12L, false, "2026-10-01T10:00:00Z");
        recentRead = save(maria, NotificationType.REVIEW_REPLIED, 12L, true, "2026-10-02T10:00:00Z");
        newest = save(maria, NotificationType.REVIEW_HIDDEN, 13L, false, "2026-10-02T12:00:00Z");
        save(ana, NotificationType.REVIEW_HELPFUL, 20L, false, "2026-10-02T13:00:00Z");
    }

    private Notification save(User user, NotificationType type, Long reviewId, boolean read, String createdAt) {
        return notificationRepository.save(Notification.builder().userId(user.getId()).type(type).title("t").message("m")
                .link("/products/x#review-" + reviewId).reviewId(reviewId).read(read).createdAt(Instant.parse(createdAt)).build());
    }

    @Test
    void findForUser_newestFirst_onlyOwnNotifications() {
        Page<Notification> page = notificationRepository.findForUser(maria.getId(), false, PageRequest.of(0, 10));

        assertEquals(3, page.getTotalElements());
        assertEquals(java.util.List.of(newest.getId(), recentRead.getId(), old.getId()),
                page.getContent().stream().map(Notification::getId).toList());
    }

    @Test
    void findForUser_unreadOnly() {
        Page<Notification> page = notificationRepository.findForUser(maria.getId(), true, PageRequest.of(0, 10));

        assertEquals(java.util.List.of(newest.getId(), old.getId()), page.getContent().stream().map(Notification::getId).toList());
        assertEquals(2, notificationRepository.countByUserIdAndReadFalse(maria.getId()));
    }

    @Test
    void markAllRead_onlyForTheUser() {
        assertEquals(2, notificationRepository.markAllRead(maria.getId()));

        assertEquals(0, notificationRepository.countByUserIdAndReadFalse(maria.getId()));
        assertEquals(1, notificationRepository.countByUserIdAndReadFalse(ana.getId()));
    }

    @Test
    void findByIdAndUserId_doesNotLeakOtherUsersNotifications() {
        assertTrue(notificationRepository.findByIdAndUserId(old.getId(), maria.getId()).isPresent());
        assertTrue(notificationRepository.findByIdAndUserId(old.getId(), ana.getId()).isEmpty());
    }

    @Test
    void findUnreadAggregated_ignoresReadOnes() {
        assertEquals(old.getId(), notificationRepository.findFirstByUserIdAndTypeAndReviewIdAndReadFalseOrderByIdDesc(
                maria.getId(), NotificationType.REVIEW_HELPFUL, 12L).orElseThrow().getId());

        old.setRead(true);
        notificationRepository.save(old);

        assertTrue(notificationRepository.findFirstByUserIdAndTypeAndReviewIdAndReadFalseOrderByIdDesc(
                maria.getId(), NotificationType.REVIEW_HELPFUL, 12L).isEmpty());
    }
}
