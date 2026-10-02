package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.ReviewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Objects;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    // directBestEffort: o sink continua ativo quando os assinantes do SSE desconectam
    private final Sinks.Many<NotificationDto> reviewSink = Sinks.many().multicast().directBestEffort();


    private final ReviewRepository reviewRepository;

    private final ProductRepository productRepository;

    private final ProductionReviewApiProducer productionReviewApiProducer;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository, ProductionReviewApiProducer productionReviewApiProducer) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.productionReviewApiProducer = productionReviewApiProducer;
    }



    @Override
    @CacheEvict(value = "review", allEntries = true)
    public Review saveReview(Review review, String nameUser) {

        Product product = getProduct(review.getProductId());

        Review saved = reviewRepository.save(review);

        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setNameUser(nameUser);
        notificationDto.setAction("criacao de comentario " + product.getName());
        notificationDto.setMessage("Comentario criado com sucesso " + review.getDescription());

        // falha na notificação não deve impedir a criação da review
        try {
            productionReviewApiProducer.sendNotification(notificationDto);
        } catch (Exception e) {
            log.warn("Falha ao enviar notificação para o Kafka: {}", e.getMessage());
        }
        reviewSink.tryEmitNext(notificationDto);

        return saved;
    }


    private Product getProduct(Long productId) {
       return productRepository.findById(productId)
               .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    @Override
    @CacheEvict(value = "review", allEntries = true)
    public Review updateReview(Review review, Long id, User currentUser) {

        getProduct(review.getProductId());

        Review current = getReview(id);

        checkOwnership(current, currentUser);

        current.setTitle(review.getTitle());
        current.setDescription(review.getDescription());
        current.setNote(review.getNote());
        current.setProductId(review.getProductId());

        return reviewRepository.save(current);
    }

    @Override
    @CacheEvict(value = "review", allEntries = true)
    public void deleteReview(Long id, User currentUser) {

        Review current = getReview(id);

        checkOwnership(current, currentUser);

        reviewRepository.deleteById(id);

    }

    /** Só o autor da review ou um ADMIN podem alterá-la. */
    private void checkOwnership(Review review, User currentUser) {
        boolean isOwner = currentUser != null && Objects.equals(review.getUserId(), currentUser.getId());
        boolean isAdmin = currentUser != null && currentUser.getRoles().stream()
                .anyMatch(role -> "ADMIN".equals(role.getName()));

        if (!isOwner && !isAdmin) {
            throw new GlobalException("Você não tem permissão para alterar esta review", HttpStatus.FORBIDDEN);
        }
    }

    @Override
    @Cacheable(value = "review" , key = "#id")
    public Review getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Review not found"));
    }

    @Override
    public Page<ReviewResponseDTO> getReviews(Pageable pageable) {
        return reviewRepository.findAllWithDetails(pageable);
    }

    @Override
    public Page<ReviewResponseDTO> getReviewsByProduct(Long productId, Pageable pageable) {
        getProduct(productId);
        return reviewRepository.findByProductIdWithDetails(productId, pageable);
    }

    @Override
    public ReviewSummaryDTO getProductSummary(Long productId) {
        getProduct(productId);

        ReviewRepository.RatingSummary summary = reviewRepository.getRatingSummary(productId);

        long total = summary != null && summary.getTotalReviews() != null ? summary.getTotalReviews() : 0L;
        double average = summary != null && summary.getAverageNote() != null ? summary.getAverageNote() : 0.0;

        return ReviewSummaryDTO.builder()
                .productId(productId)
                .totalReviews(total)
                .averageNote(Math.round(average * 10.0) / 10.0)
                .build();
    }

    public Flux<NotificationDto> getCommentStream() {
        return reviewSink.asFlux();
    }

}
