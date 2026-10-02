package com.client.productionreview.service.impl;

import com.client.productionreview.dtos.review.ReviewImageDTO;
import com.client.productionreview.exception.BadRequestException;
import com.client.productionreview.exception.GlobalException;
import com.client.productionreview.exception.IoFileException;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.event.EventType;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewImage;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.ReviewImageRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.security.CurrentUser;
import com.client.productionreview.service.DomainEventPublisher;
import com.client.productionreview.service.ReviewImageService;
import com.client.productionreview.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ReviewImageServiceImpl implements ReviewImageService {

    public static final int MAX_IMAGES_PER_REVIEW = 3;

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    static final String REVIEWS_PREFIX = "reviews/";

    /** Extensão gravada por tipo aceito. */
    static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp");

    private static final Map<String, String> SERVED_TYPES = Map.of("jpg", "image/jpeg", "jpeg", "image/jpeg",
            "png", "image/png", "webp", "image/webp", "gif", "image/gif");

    /**
     * Chaves que o /files serve: fotos de review e imagens de produto, sempre geradas pela API
     * (id numérico + UUID). Qualquer outra coisa (inclusive {@code ..}) é recusada.
     */
    private static final Pattern ALLOWED_KEY = Pattern.compile(
            "^(reviews/\\d{1,19}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)"
                    + "|product/\\d{1,19}/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}(\\.[a-z0-9]{1,5})?)$");

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final StorageService storageService;
    private final DomainEventPublisher eventPublisher;
    private final String bucketName;

    public ReviewImageServiceImpl(ReviewRepository reviewRepository, ReviewImageRepository reviewImageRepository,
                                  StorageService storageService, DomainEventPublisher eventPublisher,
                                  @Value("${minio.bucket.name:production-review}") String bucketName) {
        this.reviewRepository = reviewRepository;
        this.reviewImageRepository = reviewImageRepository;
        this.storageService = storageService;
        this.eventPublisher = eventPublisher;
        this.bucketName = bucketName;
    }

    @Override
    public ReviewImageDTO addImage(Long reviewId, MultipartFile file, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        if (user == null || !Objects.equals(review.getUserId(), user.getId())) {
            throw new GlobalException("Só o autor pode adicionar fotos à avaliação", HttpStatus.FORBIDDEN);
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Arquivo vazio");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("A foto deve ter no máximo 5 MB");
        }
        String contentType = file.getContentType() == null ? null : file.getContentType().toLowerCase();
        String extension = contentType == null ? null : EXTENSIONS.get(contentType);
        if (extension == null || !matchesSignature(file, contentType)) {
            throw new BadRequestException("Tipo de arquivo não suportado: envie JPEG, PNG ou WebP");
        }
        if (reviewImageRepository.countByReviewId(reviewId) >= MAX_IMAGES_PER_REVIEW) {
            throw new BadRequestException("Limite de " + MAX_IMAGES_PER_REVIEW + " fotos por avaliação");
        }

        String key = REVIEWS_PREFIX + reviewId + "/" + UUID.randomUUID() + "." + extension;
        try (InputStream input = file.getInputStream()) {
            storageService.uploadFile(bucketName, key, input, file.getSize(), contentType);
        } catch (IOException e) {
            throw new IoFileException("Não foi possível ler o arquivo enviado");
        }

        ReviewImage saved = reviewImageRepository.save(ReviewImage.builder()
                .reviewId(reviewId)
                .objectKey(key)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .build());
        eventPublisher.publish(EventType.REVIEW_IMAGE_ADDED, reviewId,
                nameOf(user) + " adicionou uma foto à avaliação \"" + titleOf(review) + "\"", user);
        return ReviewImageDTO.of(saved.getId(), key);
    }

    @Override
    public void deleteImage(Long reviewId, Long imageId, User user) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        boolean isOwner = user != null && Objects.equals(review.getUserId(), user.getId());
        if (!isOwner && !CurrentUser.isAdmin(user)) {
            throw new GlobalException("Você não tem permissão para alterar esta review", HttpStatus.FORBIDDEN);
        }
        ReviewImage image = reviewImageRepository.findById(imageId)
                .filter(found -> Objects.equals(found.getReviewId(), reviewId))
                .orElseThrow(() -> new NotFoundException("Foto não encontrada"));

        reviewImageRepository.delete(image);
        removeFromStorage(image.getObjectKey());
        eventPublisher.publish(EventType.REVIEW_IMAGE_REMOVED, reviewId,
                nameOf(user) + " removeu uma foto da avaliação \"" + titleOf(review) + "\"", user);
    }

    @Override
    public void deleteAllForReview(Long reviewId) {
        var images = reviewImageRepository.findByReviewIdOrderByIdAsc(reviewId);
        if (images.isEmpty()) {
            return;
        }
        reviewImageRepository.deleteByReview(reviewId);
        images.forEach(image -> removeFromStorage(image.getObjectKey()));
    }

    @Override
    public StorageService.StoredObject openFile(String key) {
        if (!isAllowedKey(key)) {
            throw new BadRequestException("Arquivo inválido");
        }
        return storageService.getFile(bucketName, key);
    }

    @Override
    public String contentTypeOf(String key) {
        int dot = key == null ? -1 : key.lastIndexOf('.');
        String extension = dot < 0 ? "" : key.substring(dot + 1).toLowerCase();
        return SERVED_TYPES.getOrDefault(extension, "application/octet-stream");
    }

    public static boolean isAllowedKey(String key) {
        return key != null && key.length() <= 255 && ALLOWED_KEY.matcher(key).matches();
    }

    private void removeFromStorage(String key) {
        try {
            storageService.deleteFile(bucketName, key);
        } catch (Exception e) {
            // o registro já saiu do banco; um arquivo órfão no bucket não afeta o site
            log.warn("Foto {} não removida do storage: {}", key, e.getMessage());
        }
    }

    /** Confere os bytes iniciais: o Content-Type do upload é informado pelo cliente. */
    static boolean matchesSignature(MultipartFile file, String contentType) {
        byte[] head;
        try (InputStream input = file.getInputStream()) {
            head = input.readNBytes(12);
        } catch (IOException e) {
            return false;
        }
        return switch (contentType) {
            case "image/jpeg" -> startsWith(head, 0xFF, 0xD8, 0xFF);
            case "image/png" -> startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "image/webp" -> head.length >= 12
                    && Arrays.equals(Arrays.copyOfRange(head, 0, 4), "RIFF".getBytes())
                    && Arrays.equals(Arrays.copyOfRange(head, 8, 12), "WEBP".getBytes());
            default -> false;
        };
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static String nameOf(User user) {
        return user != null && user.getName() != null ? user.getName().trim() : "Usuário";
    }

    private static String titleOf(Review review) {
        return review.getTitle() == null ? "" : review.getTitle().trim();
    }
}
