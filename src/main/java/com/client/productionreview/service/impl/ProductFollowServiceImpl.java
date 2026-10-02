package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.product.FollowResponseDTO;
import com.client.productionreview.dtos.product.ProductDetailDTO;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductFollow;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ProductFollowRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.security.CurrentUser;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ProductFollowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProductFollowServiceImpl implements ProductFollowService {

    private final ProductRepository productRepository;
    private final ProductFollowRepository productFollowRepository;
    private final DomainEventPublisher eventPublisher;

    public ProductFollowServiceImpl(ProductRepository productRepository, ProductFollowRepository productFollowRepository,
                                    DomainEventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.productFollowRepository = productFollowRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public FollowResponseDTO follow(Long productId, User user) {
        Product product = findProduct(productId);
        if (!productFollowRepository.existsByProductIdAndUserId(productId, user.getId())) {
            try {
                productFollowRepository.saveAndFlush(new ProductFollow(productId, user.getId()));
                eventPublisher.publish(EventType.PRODUCT_FOLLOWED, productId,
                        nameOf(user) + " passou a seguir " + nameOf(product), user);
            } catch (DataIntegrityViolationException e) {
                // clique duplo: o outro pedido já gravou
                log.debug("Produto {} já seguido pelo usuário {}", productId, user.getId());
            }
        }
        return new FollowResponseDTO(true, productFollowRepository.countByProductId(productId));
    }

    @Override
    public FollowResponseDTO unfollow(Long productId, User user) {
        Product product = findProduct(productId);
        if (productFollowRepository.deleteFollow(productId, user.getId()) > 0) {
            eventPublisher.publish(EventType.PRODUCT_UNFOLLOWED, productId,
                    nameOf(user) + " deixou de seguir " + nameOf(product), user);
        }
        return new FollowResponseDTO(false, productFollowRepository.countByProductId(productId));
    }

    @Override
    public Page<ProductSummaryDTO> following(Long userId, Pageable pageable) {
        return productRepository.findSummaries(ProductFilter.followedBy(userId), pageable);
    }

    @Override
    public ProductDetailDTO withFollowInfo(ProductDetailDTO detail) {
        if (detail == null) {
            return null;
        }
        detail.setFollowersCount(productFollowRepository.countByProductId(detail.getId()));
        Long userId = CurrentUser.id();
        detail.setFollowedByMe(userId != null && productFollowRepository.existsByProductIdAndUserId(detail.getId(), userId));
        return detail;
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(() -> new NotFoundException("Product not found"));
    }

    private static String nameOf(User user) {
        return user != null && user.getName() != null ? user.getName().trim() : "Usuário";
    }

    private static String nameOf(Product product) {
        return product.getName() == null ? "produto " + product.getId() : product.getName().trim();
    }
}
