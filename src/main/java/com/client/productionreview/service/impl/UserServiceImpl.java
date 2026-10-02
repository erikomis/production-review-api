package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.notification.NotificationPreferencesDTO;
import com.client.productionreview.dtos.user.PublicProfileDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import com.client.productionreview.service.UserService;
import com.client.productionreview.utils.RatingUtils;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {


    private final UserRepository userRepository;

    private final ReviewRepository reviewRepository;

    UserServiceImpl(UserRepository userRepository, ReviewRepository reviewRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }


    @Override
    public User me(Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            throw new NotFoundException("User not found");
        }
        return user.get();
    }

    @Override
    public NotificationPreferencesDTO getPreferences(Long userId) {
        return new NotificationPreferencesDTO(me(userId).wantsEmailNotifications());
    }

    @Override
    public NotificationPreferencesDTO updatePreferences(Long userId, boolean emailNotifications) {
        User user = me(userId);
        user.setEmailNotifications(emailNotifications);
        userRepository.save(user);
        return new NotificationPreferencesDTO(emailNotifications);
    }

    @Override
    public PublicProfileDTO getPublicProfile(String username) {
        User user = userRepository.findByUsername(username)
                .filter(found -> Boolean.TRUE.equals(found.getActive()))
                .orElseThrow(() -> new NotFoundException("User not found"));

        ReviewRepository.RatingSummary summary = reviewRepository.getUserRatingSummary(user.getId());
        long reviews = summary != null && summary.getTotalReviews() != null ? summary.getTotalReviews() : 0L;

        return PublicProfileDTO.builder()
                .username(user.getUsername() == null ? null : user.getUsername().trim())
                .name(user.getName() == null ? null : user.getName().trim())
                .memberSince(user.getCreatedAt())
                .reviewsCount(reviews)
                .helpfulReceived(reviewRepository.countHelpfulReceived(user.getId()))
                .averageNoteGiven(reviews == 0 ? null : RatingUtils.round(summary.getAverageNote()))
                .build();
    }
}
