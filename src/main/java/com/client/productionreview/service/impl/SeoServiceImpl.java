package com.client.productionreview.service.impl;

import com.client.productionreview.config.InstantJsonSerializer;
import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.dtos.seo.SeoProductDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.SeoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SeoServiceImpl implements SeoService {

    static final int MAX_REVIEWS = 5;

    /** Descrições importadas do Open Food Facts: "Marca: Nestlé · 500g · Nutri-Score A". */
    private static final Pattern BRAND = Pattern.compile("(?:^|·)\\s*Marca:\\s*([^·]+?)\\s*(?:·|$)");

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final String siteUrl;

    public SeoServiceImpl(ProductRepository productRepository, ProductImageRepository productImageRepository,
                          CategoryRepository categoryRepository, ReviewRepository reviewRepository,
                          @Value("${app.frontend-site-url:http://localhost:5174}") String siteUrl) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.siteUrl = siteUrl.endsWith("/") ? siteUrl.substring(0, siteUrl.length() - 1) : siteUrl;
    }

    @Override
    @Cacheable(value = "seo", key = "'sitemap'")
    public String sitemap() {
        StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
                .append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        url(xml, "/", null);
        url(xml, "/products", null);
        url(xml, "/ranking", null);
        categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getId))
                .forEach(category -> url(xml, "/categorias/" + category.getSlug().trim(), category.getUpdatedAt()));
        productRepository.findSlugsForSitemap()
                .forEach(product -> url(xml, "/products/" + product.getSlug().trim(), product.getUpdatedAt()));
        return xml.append("</urlset>\n").toString();
    }

    @Override
    @Cacheable(value = "seo", key = "'product:' + #slug")
    public SeoProductDTO product(String slug) {
        ProductSummaryDTO product = productRepository.findSummaries(ProductFilter.bySlug(slug), PageRequest.of(0, 1))
                .stream().findFirst()
                .orElseThrow(() -> new NotFoundException("Product not found"));

        String image = productImageRepository.findByProductIdOrderByIdAsc(product.getId()).stream()
                .findFirst().map(found -> found.getUrlImage() == null ? null : found.getUrlImage().trim()).orElse(null);

        List<SeoProductDTO.SeoReview> reviews = reviewRepository.searchDetails(ReviewSearch.builder()
                        .productId(product.getId()).status(ReviewStatus.VISIBLE).sort(ReviewSort.helpful).build(),
                PageRequest.of(0, MAX_REVIEWS)).getContent().stream()
                .map(SeoServiceImpl::toSeoReview)
                .toList();

        return SeoProductDTO.builder()
                .name(trim(product.getName()))
                .description(trim(product.getDescription()))
                .image(image)
                .brand(brandOf(product.getDescription()))
                .url(siteUrl + "/products/" + product.getSlug().trim())
                .aggregateRating(product.getTotalReviews() == 0 ? null
                        : new SeoProductDTO.AggregateRating(product.getAverageNote(), product.getTotalReviews()))
                .reviews(new java.util.ArrayList<>(reviews))
                .build();
    }

    private void url(StringBuilder xml, String path, Instant lastModified) {
        xml.append("  <url><loc>").append(HtmlUtils.htmlEscape(siteUrl + path)).append("</loc>");
        if (lastModified != null) {
            xml.append("<lastmod>").append(InstantJsonSerializer.format(lastModified)).append("</lastmod>");
        }
        xml.append("</url>\n");
    }

    private static SeoProductDTO.SeoReview toSeoReview(ReviewResponseDTO review) {
        return new SeoProductDTO.SeoReview(trim(review.getUserName()), review.getCreatedAt(), trim(review.getDescription()),
                trim(review.getTitle()), review.getNote());
    }

    public static String brandOf(String description) {
        if (description == null) {
            return null;
        }
        Matcher matcher = BRAND.matcher(description);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
