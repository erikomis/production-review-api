package com.client.productionreview.service;

import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import com.client.productionreview.dtos.review.ReviewSort;
import com.client.productionreview.dtos.seo.SeoProductDTO;
import com.client.productionreview.exception.NotFoundException;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.service.impl.SeoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeoServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ReviewRepository reviewRepository;

    private SeoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SeoServiceImpl(productRepository, productImageRepository, categoryRepository, reviewRepository,
                "https://site.com/");
    }

    private ProductSummaryDTO summary(long totalReviews, Double average) {
        return new ProductSummaryDTO(3L, "Café Pilão ", "Marca: Pilão · 500g · Nutri-Score B", "cafe-pilao",
                1L, "Cafés", "cafes", 1L, "Bebidas", "bebidas", Instant.parse("2026-10-01T10:00:00Z"), average, totalReviews);
    }

    private ProductRepository.SlugUpdated slug(String slug, Instant updatedAt) {
        return new ProductRepository.SlugUpdated() {
            @Override
            public String getSlug() {
                return slug;
            }

            @Override
            public Instant getUpdatedAt() {
                return updatedAt;
            }
        };
    }

    @Test
    void sitemap_listsStaticPagesCategoriesAndProducts() {
        when(categoryRepository.findAll()).thenReturn(List.of(Category.builder().id(1L).slug("bebidas ")
                .updatedAt(Instant.parse("2026-09-30T08:00:00Z")).build()));
        when(productRepository.findSlugsForSitemap()).thenReturn(List.of(
                slug("cafe-pilao", Instant.parse("2026-10-02T02:14:49Z")), slug("leite&cia", null)));

        String xml = service.sitemap();

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
        assertTrue(xml.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"));
        assertTrue(xml.contains("<url><loc>https://site.com/</loc></url>"));
        assertTrue(xml.contains("<url><loc>https://site.com/products</loc></url>"));
        assertTrue(xml.contains("<url><loc>https://site.com/ranking</loc></url>"));
        assertTrue(xml.contains("<url><loc>https://site.com/categorias/bebidas</loc><lastmod>2026-09-30T08:00:00Z</lastmod></url>"));
        assertTrue(xml.contains("<url><loc>https://site.com/products/cafe-pilao</loc><lastmod>2026-10-02T02:14:49Z</lastmod></url>"));
        // XML escapado
        assertTrue(xml.contains("<loc>https://site.com/products/leite&amp;cia</loc>"));
        assertTrue(xml.trim().endsWith("</urlset>"));
    }

    @Test
    void product_buildsJsonLdData_withTopHelpfulReviews() {
        when(productRepository.findSummaries(any(), any())).thenReturn(new PageImpl<>(List.of(summary(2, 4.5))));
        ProductImage image = new ProductImage();
        image.setUrlImage("https://img/cafe.jpg");
        when(productImageRepository.findByProductIdOrderByIdAsc(3L)).thenReturn(List.of(image));
        when(reviewRepository.searchDetails(any(), any())).thenReturn(new PageImpl<>(List.of(ReviewResponseDTO.builder()
                .id(1L).userName("Maria ").title("Ótimo").description("Muito bom").note(5L)
                .createdAt(Instant.parse("2026-10-01T12:00:00Z")).build())));

        SeoProductDTO seo = service.product("cafe-pilao");

        assertEquals("Café Pilão", seo.getName());
        assertEquals("Pilão", seo.getBrand());
        assertEquals("https://img/cafe.jpg", seo.getImage());
        assertEquals("https://site.com/products/cafe-pilao", seo.getUrl());
        assertEquals(4.5, seo.getAggregateRating().getRatingValue());
        assertEquals(2, seo.getAggregateRating().getReviewCount());
        assertEquals(1, seo.getReviews().size());
        assertEquals("Maria", seo.getReviews().get(0).getAuthor());
        assertEquals("Muito bom", seo.getReviews().get(0).getReviewBody());
        assertEquals("Ótimo", seo.getReviews().get(0).getName());
        assertEquals(5L, seo.getReviews().get(0).getRatingValue());

        ArgumentCaptor<ReviewSearch> search = ArgumentCaptor.forClass(ReviewSearch.class);
        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).searchDetails(search.capture(), page.capture());
        assertEquals(ReviewSort.helpful, search.getValue().sort());
        assertEquals(5, page.getValue().getPageSize());
    }

    @Test
    void product_withoutReviews_hasNullAggregateRating() {
        when(productRepository.findSummaries(any(), any())).thenReturn(new PageImpl<>(List.of(summary(0, null))));
        when(reviewRepository.searchDetails(any(), any())).thenReturn(new PageImpl<>(List.of()));

        SeoProductDTO seo = service.product("cafe-pilao");

        assertNull(seo.getAggregateRating());
        assertNull(seo.getImage());
        assertTrue(seo.getReviews().isEmpty());
    }

    @Test
    void product_missing_is404() {
        when(productRepository.findSummaries(any(), any())).thenReturn(new PageImpl<>(List.of()));

        assertThrows(NotFoundException.class, () -> service.product("x"));
    }

    @Test
    void brandOf_parsesImportedDescriptions() {
        assertEquals("Nestlé", SeoServiceImpl.brandOf("Marca: Nestlé · 500g"));
        assertEquals("Pilão", SeoServiceImpl.brandOf("Marca: Pilão"));
        assertNull(SeoServiceImpl.brandOf("Bebida láctea fermentada"));
        assertNull(SeoServiceImpl.brandOf(null));
    }
}
