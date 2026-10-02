package com.client.productionreview.controller;

import com.client.productionreview.service.ReviewImageService;
import com.client.productionreview.service.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Proxy público dos arquivos do storage (o bucket é privado). Ex.: {@code GET /api/v1/files/reviews/12/uuid.jpg}.
 * O conteúdo é repassado em streaming; a chave é validada contra os prefixos permitidos.
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    static final String PREFIX = "/api/v1/files/";

    private final ReviewImageService reviewImageService;

    public FileController(ReviewImageService reviewImageService) {
        this.reviewImageService = reviewImageService;
    }

    @GetMapping("/**")
    public ResponseEntity<InputStreamResource> getFile(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String key = path.startsWith(PREFIX) ? path.substring(PREFIX.length()) : "";

        StorageService.StoredObject object = reviewImageService.openFile(key);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(reviewImageService.contentTypeOf(key)))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic());
        if (object.size() >= 0) {
            response.contentLength(object.size());
        }
        return response.body(new InputStreamResource(object.content()));
    }
}
