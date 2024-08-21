package com.client.productionreview.service.impl;

import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.Product;
import com.client.productionreview.model.Review;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.ReviewService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {


    ReviewRepository reviewRepository;

    ProductRepository productRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
    }



    @Override
    public Review saveReview(Review review) {

        Optional<Product> product = getProduct(review);

        if(product.isEmpty()){
            throw new NotFoundException("Product not found");
        }

        return reviewRepository.save(review);

    }

    private Optional<Product> getProduct(Review review) {
        Optional<Product> product = productRepository.findById(review.getProductId());
        return product;
    }

    @Override
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
    public void deleteReview(Long id) {

        Optional<Review> reviewOptional = getOptionalReview(id);

        if(reviewOptional.isEmpty()){
            throw new NotFoundException("Review not found");
        }

        reviewRepository.deleteById(id);

    }

    @Override
    public Review getReview(Long id) {

        Optional<Review> reviewOptional = getOptionalReview(id);

        if(reviewOptional.isEmpty()){
                throw new NotFoundException("Review not found");
            }
        return reviewOptional.get();
    }

    private Optional<Review> getOptionalReview(Long id) {
        Optional<Review> reviewOptional = reviewRepository.findById(id);
        return reviewOptional;
    }

    @Override
    public List<Review> getReviews() {
        return reviewRepository.findAll();

    }
}
