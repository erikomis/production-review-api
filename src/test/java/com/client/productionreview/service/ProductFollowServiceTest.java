package com.client.productionreview.service;

import com.client.productionreview.dtos.product.FollowResponseDTO;
import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.service.impl.ProductFollowServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductFollowServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductFollowRepository productFollowRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private ProductFollowServiceImpl service;

    private final User user = User.builder().id(2L).name("Maria").build();
    private final Product product = Product.builder().id(3L).name("Café").build();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void follow_createsOnceAndReturnsCount() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(productFollowRepository.existsByProductIdAndUserId(3L, 2L)).thenReturn(false, true);
        when(productFollowRepository.countByProductId(3L)).thenReturn(4L);

        FollowResponseDTO first = service.follow(3L, user);
        FollowResponseDTO second = service.follow(3L, user);

        assertTrue(first.isFollowing());
        assertEquals(4L, first.getFollowersCount());
        assertTrue(second.isFollowing());
        verify(productFollowRepository, times(1)).saveAndFlush(any());
        verify(eventPublisher, times(1)).publish(eq(EventType.PRODUCT_FOLLOWED), eq(3L), contains("passou a seguir Café"), eq(user));
    }

    @Test
    void follow_concurrentDuplicate_staysFollowing() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(productFollowRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("pk"));

        assertTrue(service.follow(3L, user).isFollowing());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void unfollow_publishesOnlyWhenSomethingChanged() {
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(productFollowRepository.deleteFollow(3L, 2L)).thenReturn(1, 0);
        when(productFollowRepository.countByProductId(3L)).thenReturn(3L);

        FollowResponseDTO result = service.unfollow(3L, user);
        service.unfollow(3L, user);

        assertFalse(result.isFollowing());
        assertEquals(3L, result.getFollowersCount());
        verify(eventPublisher, times(1)).publish(eq(EventType.PRODUCT_UNFOLLOWED), eq(3L), anyString(), eq(user));
    }

    @Test
    void follow_missingProduct_is404() {
        when(productRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.follow(9L, user));
        assertThrows(NotFoundException.class, () -> service.unfollow(9L, user));
    }

    @Test
    void following_usesFollowedByFilter() {
        when(productRepository.findSummaries(any(), any())).thenReturn(new PageImpl<>(List.of()));

        service.following(2L, PageRequest.of(0, 10));

        verify(productRepository).findSummaries(eq(ProductFilter.followedBy(2L)), any());
    }

    @Test
    void withFollowInfo_fillsCountAndFollowedByMe() {
        ProductDetailDTO detail = new ProductDetailDTO();
        detail.setId(3L);
        when(productFollowRepository.countByProductId(3L)).thenReturn(4L);

        service.withFollowInfo(detail);
        assertEquals(4L, detail.getFollowersCount());
        assertFalse(detail.isFollowedByMe());

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        when(productFollowRepository.existsByProductIdAndUserId(3L, 2L)).thenReturn(true);
        service.withFollowInfo(detail);
        assertTrue(detail.isFollowedByMe());
        assertNull(service.withFollowInfo(null));
    }
}
