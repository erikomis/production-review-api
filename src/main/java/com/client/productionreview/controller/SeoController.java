package com.client.productionreview.controller;

import com.client.productionreview.dtos.seo.SeoProductDTO;
import com.client.productionreview.service.SeoService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/** Dados de SEO para o site (rotas públicas). */
@RestController
@RequestMapping("/api/v1/seo")
public class SeoController {

    private final SeoService seoService;

    public SeoController(SeoService seoService) {
        this.seoService = seoService;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(seoService.sitemap());
    }

    @GetMapping("/products/{slug}")
    public SeoProductDTO product(@PathVariable("slug") String slug) {
        return seoService.product(slug);
    }
}
