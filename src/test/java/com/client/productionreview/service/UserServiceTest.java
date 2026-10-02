package com.client.productionreview.service;

import com.client.productionreview.dtos.user.PublicProfileDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private com.client.productionreview.service.impl.UserServiceImpl service;

    private User maria;

    @BeforeEach
    void setUp() {
        maria = User.builder().id(2L).name("Maria ").username("maria").email("maria@mail.com").active(true)
                .createdAt(Instant.parse("2026-09-01T12:00:00Z")).build();
    }

    private ReviewRepository.RatingSummary summary(Long total, Double average) {
        return new ReviewRepository.RatingSummary() {
            @Override
            public Long getTotalReviews() {
                return total;
            }

            @Override
            public Double getAverageNote() {
                return average;
            }
        };
    }

    @Test
    void publicProfile_hasStatsAndNoEmail() {
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(maria));
        when(reviewRepository.getUserRatingSummary(2L)).thenReturn(summary(3L, 4.3333));
        when(reviewRepository.countHelpfulReceived(2L)).thenReturn(7L);

        PublicProfileDTO profile = service.getPublicProfile("maria");

        assertEquals("maria", profile.getUsername());
        assertEquals("Maria", profile.getName());
        assertEquals(Instant.parse("2026-09-01T12:00:00Z"), profile.getMemberSince());
        assertEquals(3, profile.getReviewsCount());
        assertEquals(7, profile.getHelpfulReceived());
        assertEquals(4.3, profile.getAverageNoteGiven());
    }

    @Test
    void publicProfile_withoutReviews_hasNullAverage() {
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(maria));
        when(reviewRepository.getUserRatingSummary(2L)).thenReturn(summary(0L, null));

        assertNull(service.getPublicProfile("maria").getAverageNoteGiven());
    }

    @Test
    void publicProfile_inactiveOrMissing_is404() {
        maria.setActive(false);
        when(userRepository.findByUsername("maria")).thenReturn(Optional.of(maria));
        when(userRepository.findByUsername("x")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getPublicProfile("maria"));
        assertThrows(NotFoundException.class, () -> service.getPublicProfile("x"));
    }

    @Test
    void preferences_defaultTrue_andUpdate() {
        maria.setEmailNotifications(null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(maria));

        assertTrue(service.getPreferences(2L).getEmailNotifications());

        assertFalse(service.updatePreferences(2L, false).getEmailNotifications());
        assertFalse(maria.getEmailNotifications());
        verify(userRepository).save(maria);
    }
}
