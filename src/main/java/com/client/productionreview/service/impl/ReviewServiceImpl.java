package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.message.producer.ProductionReviewApiProducer;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.ReviewService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final Sinks.Many<NotificationDto> reviewSink = Sinks.many().multicast().onBackpressureBuffer();


    ReviewRepository reviewRepository;

    ProductRepository productRepository;

    ProductionReviewApiProducer productionReviewApiProducer;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository, ProductionReviewApiProducer productionReviewApiProducer) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.productionReviewApiProducer = productionReviewApiProducer;
    }



    @Override
    @CacheEvict(value = "review", allEntries = true)
    public Review saveReview(Review review, String nameUser) {

        Optional<Product> product = getProduct(review);

        if(product.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        NotificationDto notificationDto = new NotificationDto();
        notificationDto.setNameUser(nameUser);
        notificationDto.setAction("criacao de comentario " + product.get().getName());
        notificationDto.setMessage("Comentario criado com sucesso " + review.getDescription());

        productionReviewApiProducer.sendNotification(notificationDto);
        reviewSink.tryEmitNext(notificationDto);

        return reviewRepository.save(review);


    }


    private Optional<Product> getProduct(Review review) {
       return productRepository.findById(review.getProductId());
    }

    @Override
    @CacheEvict(value = "review", allEntries = true, key = "#id")
    public Review updateReview(Review review, Long id) {

        Optional<Product> product = getProduct(review);

        if(product.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        Optional<Review> reviewOptional = getOptionalReview(id);

        if(reviewOptional.isEmpty()){
            throw new NotFoundException("Review not found");
        }


        return reviewRepository.save(review);


    }

    @Override
    @CacheEvict(value = "review", allEntries = true, key = "#id")
    public void deleteReview(Long id) {

        Optional<Review> reviewOptional = getOptionalReview(id);

        if(reviewOptional.isEmpty()){
            throw new NotFoundException("Review not found");
        }


        reviewRepository.deleteById(id);

    }

    @Override
    @Cacheable(value = "review" , key = "#id")
    public Review getReview(Long id) {

        Optional<Review> reviewOptional = getOptionalReview(id);

        if(reviewOptional.isEmpty()){
                throw new NotFoundException("Review not found");
            }
        return reviewOptional.get();
    }

    private Optional<Review> getOptionalReview(Long id) {
        return reviewRepository.findById(id);

    }

    @Cacheable(value = "review")
    @Override
    public List<Review> getReviews() {
        return reviewRepository.findAll();

    }

    public Flux<NotificationDto> getCommentStream() {
        return reviewSink.asFlux();
    }

}
