package com.client.productionreview.service;

import com.client.productionreview.dtos.review.ReviewImageDTO;
import com.client.productionreview.model.jpa.User;
import org.springframework.web.multipart.MultipartFile;

public interface ReviewImageService {

    /** Só o autor; JPEG/PNG/WebP de até 5 MB; no máximo 3 fotos por review. */
    ReviewImageDTO addImage(Long reviewId, MultipartFile file, User user);

    /** Autor ou ADMIN. */
    void deleteImage(Long reviewId, Long imageId, User user);

    /** Remove todas as fotos da review (arquivos no storage em melhor esforço). */
    void deleteAllForReview(Long reviewId);

    /** Abre um arquivo público servido por /files; a chave precisa estar num prefixo permitido. */
    StorageService.StoredObject openFile(String key);

    /** Content-Type servido para a chave (pela extensão, nunca pelo que veio no upload). */
    String contentTypeOf(String key);
}
