package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.NotificationDto;
import com.client.productionreview.dtos.review.HelpfulResponseDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.dtos.review.ReviewSummaryDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewHelpful;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewHelpfulRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.security.CurrentUser;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ReviewService;
import com.client.productionreview.utils.RatingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class ReviewServiceImpl implements ReviewService {

    // directBestEffort: o sink continua ativo quando os assinantes do SSE desconectam
    private final Sinks.Many<NotificationDto> reviewSink = Sinks.many().multicast().directBestEffort();


    private final ReviewRepository reviewRepository;

    private final ProductRepository productRepository;

    private final ReviewHelpfulRepository reviewHelpfulRepository;

    private final DomainEventPublisher eventPublisher;

    public ReviewServiceImpl(ReviewRepository reviewRepository, ProductRepository productRepository,
                             ReviewHelpfulRepository reviewHelpfulRepository, DomainEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.productRepository = productRepository;
        this.reviewHelpfulRepository = reviewHelpfulRepository;
        this.eventPublisher = eventPublisher;
    }



    // a nota média e o total aparecem na listagem e no detalhe de produtos
    @Override
    @CacheEvict(value = {"review", "product"}, allEntries = true)
    public Review saveReview(Review review, String nameUser) {

        Product product = getProduct(review.getProductId());

        review.setStatus(ReviewStatus.VISIBLE);
        Review saved = reviewRepository.save(review);

        User actor = CurrentUser.get()
                .orElseGet(() -> User.builder().id(review.getUserId()).name(nameUser).build());
        long note = review.getNote() == null ? 0 : review.getNote();
        String message = nameUser + " avaliou " + product.getName() + " com " + note + (note == 1 ? " estrela" : " estrelas");

        NotificationDto event = eventPublisher.publish(EventType.REVIEW_CREATED, saved.getId(), message, actor);
        if (event == null) {
            event = new NotificationDto(EventType.REVIEW_CREATED.getAction(), message, nameUser);
        }
        reviewSink.tryEmitNext(event);

        return saved;
    }


    private Product getProduct(Long productId) {
       return productRepository.findById(productId)
               .orElseThrow(() -> new NotFoundException("Product not found"));
    }

    // editar não muda o status de moderação
    @Override
    @CacheEvict(value = {"review", "product"}, allEntries = true)
    public Review updateReview(Review review, Long id, User currentUser) {

        Product product = getProduct(review.getProductId());

        Review current = getReview(id);

        checkOwnership(current, currentUser);

        current.setTitle(review.getTitle());
        current.setDescription(review.getDescription());
        current.setNote(review.getNote());
        current.setProductId(review.getProductId());

        Review saved = reviewRepository.save(current);
        eventPublisher.publish(EventType.REVIEW_UPDATED, id,
                nameOf(currentUser) + " editou a avaliação de " + product.getName());
        return saved;
    }

    @Override
    @CacheEvict(value = {"review", "product"}, allEntries = true)
    public void deleteReview(Long id, User currentUser) {

        Review current = getReview(id);

        checkOwnership(current, currentUser);

        // o H2 dos testes não tem o ON DELETE CASCADE da migração
        reviewHelpfulRepository.deleteByReview(id);
        reviewRepository.deleteById(id);

        String productName = productRepository.findById(current.getProductId())
                .map(Product::getName).orElse("produto " + current.getProductId());
        eventPublisher.publish(EventType.REVIEW_DELETED, id,
                nameOf(currentUser) + " excluiu a avaliação de " + productName);
    }

    private static String nameOf(User user) {
        return user != null && user.getName() != null ? user.getName().trim() : "Usuário";
    }

    /** Só o autor da review ou um ADMIN podem alterá-la. */
    private void checkOwnership(Review review, User currentUser) {
        boolean isOwner = currentUser != null && Objects.equals(review.getUserId(), currentUser.getId());

        if (!isOwner && !CurrentUser.isAdmin(currentUser)) {
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
    public ReviewResponseDTO getReviewDetails(Long id) {
        ReviewResponseDTO dto = reviewRepository.searchDetails(ReviewSearch.builder().reviewId(id).build(), Pageable.ofSize(1))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Review not found"));

        User current = CurrentUser.get().orElse(null);
        boolean canSeeHidden = current != null
                && (Objects.equals(current.getId(), dto.getUserId()) || CurrentUser.isAdmin(current));
        if (dto.getStatus() == ReviewStatus.HIDDEN && !canSeeHidden) {
            throw new NotFoundException("Review not found");
        }

        fillHelpfulByMe(List.of(dto));
        return dto;
    }

    @Override
    public Page<ReviewResponseDTO> getReviews(Pageable pageable) {
        Sort.Order order = pageable.getSort().getOrderFor("createdAt");
        ReviewSort sort = order != null && order.isAscending() ? ReviewSort.oldest : ReviewSort.recent;

        return withHelpfulByMe(reviewRepository.searchDetails(
                ReviewSearch.builder().status(ReviewStatus.VISIBLE).sort(sort).build(), pageable));
    }

    @Override
    public Page<ReviewResponseDTO> getReviewsByProduct(Long productId, Long note, String sort, Pageable pageable) {
        ReviewSort reviewSort = ReviewSort.from(sort);
        if (note != null && (note < 1 || note > 5)) {
            throw new BadRequestException("A nota deve estar entre 1 e 5");
        }
        getProduct(productId);

        return withHelpfulByMe(reviewRepository.searchDetails(ReviewSearch.builder()
                .productId(productId).status(ReviewStatus.VISIBLE).note(note).sort(reviewSort).build(), pageable));
    }

    @Override
    public Page<ReviewResponseDTO> getMyReviews(Long userId, Pageable pageable) {
        return withHelpfulByMe(reviewRepository.searchDetails(
                ReviewSearch.builder().userId(userId).sort(ReviewSort.recent).build(), pageable));
    }

    private Page<ReviewResponseDTO> withHelpfulByMe(Page<ReviewResponseDTO> page) {
        fillHelpfulByMe(page.getContent());
        return page;
    }

    private void fillHelpfulByMe(List<ReviewResponseDTO> reviews) {
        Long userId = CurrentUser.id();
        if (userId == null || reviews.isEmpty()) {
            return;
        }
        Set<Long> marked = new HashSet<>(reviewHelpfulRepository.findReviewIdsMarkedBy(userId,
                reviews.stream().map(ReviewResponseDTO::getId).toList()));
        reviews.forEach(review -> review.setHelpfulByMe(marked.contains(review.getId())));
    }

    @Override
    public ReviewSummaryDTO getProductSummary(Long productId) {
        getProduct(productId);

        ReviewRepository.RatingSummary summary = reviewRepository.getRatingSummary(productId);

        long total = summary != null && summary.getTotalReviews() != null ? summary.getTotalReviews() : 0L;
        double average = summary != null && summary.getAverageNote() != null ? summary.getAverageNote() : 0.0;

        Map<String, Long> distribution = RatingUtils.emptyDistribution();
        List<ReviewRepository.NoteCount> counts = reviewRepository.countByNoteForProduct(productId);
        if (counts != null) {
            counts.stream()
                    .filter(count -> count.getNote() != null && distribution.containsKey(String.valueOf(count.getNote())))
                    .forEach(count -> distribution.put(String.valueOf(count.getNote()), count.getTotal()));
        }

        return ReviewSummaryDTO.builder()
                .productId(productId)
                .totalReviews(total)
                .averageNote(RatingUtils.round(average))
                .distribution(distribution)
                .build();
    }

    @Override
    public HelpfulResponseDTO toggleHelpful(Long reviewId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .filter(found -> found.getStatus() != ReviewStatus.HIDDEN)
                .orElseThrow(() -> new NotFoundException("Review not found"));

        if (Objects.equals(review.getUserId(), user.getId())) {
            throw new BadRequestException("Você não pode marcar a sua própria avaliação como útil");
        }

        boolean helpfulByMe;
        if (reviewHelpfulRepository.existsByReviewIdAndUserId(reviewId, user.getId())) {
            reviewHelpfulRepository.deleteMark(reviewId, user.getId());
            helpfulByMe = false;
        } else {
            try {
                reviewHelpfulRepository.saveAndFlush(new ReviewHelpful(reviewId, user.getId()));
            } catch (DataIntegrityViolationException e) {
                // clique duplo/requisições simultâneas: a marcação já foi gravada pela outra
                log.debug("Marcação de útil já existente para review {} e usuário {}", reviewId, user.getId());
            }
            helpfulByMe = true;
        }

        return new HelpfulResponseDTO(reviewId, reviewHelpfulRepository.countByReviewId(reviewId), helpfulByMe);
    }

    public Flux<NotificationDto> getCommentStream() {
        return reviewSink.asFlux();
    }

}
