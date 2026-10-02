package com.client.productionreview.service;

import com.client.productionreview.dtos.notification.NotificationResponseDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.integration.NotificationMailer;
import com.client.productionreview.model.jpa.Notification;
import com.client.productionreview.model.jpa.NotificationType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.NotificationRepository;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductFollowRepository productFollowRepository;
    @Mock
    private NotificationMailer mailer;

    private NotificationServiceImpl service;
    private Review review;
    private Product product;
    private User author;

    @BeforeEach
    void setUp() {
        service = new NotificationServiceImpl(notificationRepository, userRepository, productRepository,
                productFollowRepository, mailer, "http://site.local/");
        product = Product.builder().id(3L).name("Café Pilão").slug("cafe-pilao ").build();
        review = Review.builder().id(12L).userId(2L).productId(3L).title("Ótimo café").note(5L).build();
        author = User.builder().id(2L).name("Maria").email("maria@mail.com").active(true).emailNotifications(true).build();
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(userRepository.findById(2L)).thenReturn(Optional.of(author));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Notification saved() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void reviewHidden_createsNotificationAndSendsEmail() {
        service.reviewHidden(review, "spam");

        Notification notification = saved();
        assertEquals(2L, notification.getUserId());
        assertEquals(NotificationType.REVIEW_HIDDEN, notification.getType());
        assertEquals("/minhas-avaliacoes", notification.getLink());
        assertTrue(notification.getMessage().contains("Motivo: spam"));
        assertFalse(notification.isRead());
        assertNotNull(notification.getCreatedAt());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq("maria@mail.com"), eq("Sua avaliação foi ocultada - ReviewStore"), body.capture());
        assertTrue(body.getValue().contains("http://site.local/minhas-avaliacoes"));
    }

    @Test
    void reviewReplied_linksToTheReviewOnTheProductPage() {
        service.reviewReplied(review, "Obrigado!");

        Notification notification = saved();
        assertEquals(NotificationType.REVIEW_REPLIED, notification.getType());
        assertEquals("/products/cafe-pilao#review-12", notification.getLink());
        verify(mailer).send(eq("maria@mail.com"), anyString(), contains("http://site.local/products/cafe-pilao#review-12"));
    }

    @Test
    void email_respectsUserPreference() {
        author.setEmailNotifications(false);

        service.reviewReplied(review, "Obrigado!");

        verify(notificationRepository).save(any());
        verifyNoInteractions(mailer);
    }

    @Test
    void restoredAndHelpful_doNotSendEmail() {
        service.reviewRestored(review);
        service.reviewHelpful(review, 1);

        verify(notificationRepository, times(2)).save(any());
        verifyNoInteractions(mailer);
    }

    @Test
    void reviewHelpful_aggregatesIntoTheUnreadNotification() {
        Notification unread = Notification.builder().id(9L).userId(2L).type(NotificationType.REVIEW_HELPFUL).reviewId(12L)
                .title("Sua avaliação foi útil").message("1 pessoa achou útil").createdAt(Instant.parse("2026-10-01T10:00:00Z"))
                .build();
        when(notificationRepository.findFirstByUserIdAndTypeAndReviewIdAndReadFalseOrderByIdDesc(2L,
                NotificationType.REVIEW_HELPFUL, 12L)).thenReturn(Optional.of(unread));

        service.reviewHelpful(review, 3);

        verify(notificationRepository, times(1)).save(unread);
        assertEquals("3 pessoas acharam útil a sua avaliação \"Ótimo café\".", unread.getMessage());
        assertTrue(unread.getCreatedAt().isAfter(Instant.parse("2026-10-01T10:00:00Z")));
    }

    @Test
    void reviewHelpful_withoutUnread_createsNew() {
        service.reviewHelpful(review, 1);

        Notification notification = saved();
        assertNull(notification.getId());
        assertEquals("1 pessoa achou útil a sua avaliação \"Ótimo café\".", notification.getMessage());
    }

    @Test
    void followedProductReview_notifiesFollowersExceptTheAuthor() {
        User follower = User.builder().id(5L).name("Ana").email("ana@mail.com").active(true).build();
        when(userRepository.findById(5L)).thenReturn(Optional.of(follower));
        when(productFollowRepository.findFollowerIds(3L)).thenReturn(List.of(2L, 5L));

        service.followedProductReview(review, product, "Maria");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        assertEquals(5L, captor.getValue().getUserId());
        assertEquals(NotificationType.FOLLOWED_PRODUCT_REVIEW, captor.getValue().getType());
        assertEquals("Nova avaliação em Café Pilão", captor.getValue().getTitle());
        verify(mailer).send(eq("ana@mail.com"), anyString(), anyString());
        verify(mailer, never()).send(eq("maria@mail.com"), anyString(), anyString());
    }

    @Test
    void inactiveRecipient_getsNoEmail() {
        author.setActive(false);

        service.reviewHidden(review, "spam");

        verifyNoInteractions(mailer);
    }

    @Test
    void failures_neverPropagate() {
        when(notificationRepository.save(any())).thenThrow(new IllegalStateException("db"));

        assertDoesNotThrow(() -> service.reviewHidden(review, "spam"));
        assertDoesNotThrow(() -> service.reviewHelpful(review, 2));
        assertDoesNotThrow(() -> service.followedProductReview(review, product, "x"));
    }

    @Test
    void list_mapsFields() {
        Notification notification = Notification.builder().id(1L).userId(2L).type(NotificationType.REVIEW_REPLIED)
                .title("t").message("m").link("/l").read(true).createdAt(Instant.parse("2026-10-02T02:14:49Z")).build();
        when(notificationRepository.findForUser(eq(2L), eq(true), any())).thenReturn(new PageImpl<>(List.of(notification)));

        NotificationResponseDTO dto = service.list(2L, true, PageRequest.of(0, 10)).getContent().get(0);

        assertEquals(1L, dto.getId());
        assertEquals(NotificationType.REVIEW_REPLIED, dto.getType());
        assertTrue(dto.isRead());
        assertEquals("/l", dto.getLink());
    }

    @Test
    void markRead_ofAnotherUser_is404() {
        when(notificationRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.markRead(1L, 2L));
    }

    @Test
    void markRead_andMarkAll() {
        Notification notification = Notification.builder().id(1L).userId(2L).build();
        when(notificationRepository.findByIdAndUserId(1L, 2L)).thenReturn(Optional.of(notification));

        service.markRead(1L, 2L);
        service.markAllRead(2L);

        assertTrue(notification.isRead());
        verify(notificationRepository).markAllRead(2L);
    }
}
