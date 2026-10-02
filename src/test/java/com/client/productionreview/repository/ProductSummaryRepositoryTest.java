package com.client.productionreview.repository;

import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.Category;
import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.Review;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.model.jpa.SubCategory;
import com.client.productionreview.model.jpa.User;
import com.client.productionreview.repositories.jpa.CategoryRepository;
import com.client.productionreview.repositories.jpa.ProductImageRepository;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.repositories.jpa.ReviewRepository;
import com.client.productionreview.repositories.jpa.SubCategoryRepository;
import com.client.productionreview.repositories.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Roda as consultas de verdade no H2: notas, filtros e ordenações da listagem de produtos. */
@DataJpaTest
@ActiveProfiles(profiles = "test")
class ProductSummaryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SubCategoryRepository subCategoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProductImageRepository productImageRepository;
    @Autowired
    private ReviewRepository reviewRepository;
    @Autowired
    private UserRepository userRepository;

    private Category electronics;
    private SubCategory phones;
    private SubCategory notebooks;
    private SubCategory kitchen;
    private Product phoneA;
    private Product phoneB;
    private Product laptop;
    private Product coffee;
    private Product toaster;
    private User user;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(Category.builder().name("Eletrônicos").description("d").slug("eletronicos").build());
        Category home = categoryRepository.save(Category.builder().name("Casa").description("d").slug("casa").build());
        phones = sub("Celulares", "celulares", electronics);
        notebooks = sub("Notebooks", "notebooks", electronics);
        kitchen = sub("Cozinha", "cozinha", home);

        phoneA = product("Smartphone A", "smartphone-a", phones);
        phoneB = product("Smartphone B", "smartphone-b", phones);
        laptop = product("Notebook Pro", "notebook-pro", notebooks);
        coffee = product("Cafeteira", "cafeteira", kitchen);
        toaster = product("Torradeira", "torradeira", kitchen);

        user = userRepository.save(User.builder().name("u").email("u@mail.com").username("u").password("p").active(true).build());

        // phoneA: 5,4 -> 4.5 (2) | phoneB: 5,4,5,4 -> 4.5 (4) | laptop: 3 -> 3.0 | coffee: 5 + 1 oculta -> 5.0 (1)
        review(phoneA, 5, ReviewStatus.VISIBLE);
        review(phoneA, 4, ReviewStatus.VISIBLE);
        review(phoneB, 5, ReviewStatus.VISIBLE);
        review(phoneB, 4, ReviewStatus.VISIBLE);
        review(phoneB, 5, ReviewStatus.VISIBLE);
        review(phoneB, 4, ReviewStatus.VISIBLE);
        review(laptop, 3, ReviewStatus.VISIBLE);
        review(coffee, 5, ReviewStatus.VISIBLE);
        review(coffee, 1, ReviewStatus.HIDDEN);
        // torradeira só tem review oculta: conta como sem nota
        review(toaster, 2, ReviewStatus.HIDDEN);
    }

    private SubCategory sub(String name, String slug, Category category) {
        return subCategoryRepository.save(SubCategory.builder().name(name).description("d").slug(slug)
                .categorieId(category.getId()).build());
    }

    private Product product(String name, String slug, SubCategory sub) {
        return productRepository.save(Product.builder().name(name).description("d").slug(slug).subCategorieId(sub.getId()).build());
    }

    private void review(Product product, long note, ReviewStatus status) {
        reviewRepository.save(Review.builder().title("t").description("d").note(note).productId(product.getId())
                .userId(user.getId()).status(status).build());
    }

    private void image(Product product, String url) {
        ProductImage image = new ProductImage();
        image.setProductId(product.getId());
        image.setUrlImage(url);
        image.setType("image/jpeg");
        image.setFilename("f");
        productImageRepository.save(image);
    }

    private Page<ProductSummaryDTO> list(ProductFilter filter, Sort sort) {
        return productRepository.findSummaries(filter, PageRequest.of(0, 10, sort));
    }

    private List<String> names(Page<ProductSummaryDTO> page) {
        return page.getContent().stream().map(ProductSummaryDTO::getName).toList();
    }

    private ProductSummaryDTO find(Page<ProductSummaryDTO> page, Product product) {
        return page.getContent().stream().filter(p -> p.getId().equals(product.getId())).findFirst().orElseThrow();
    }

    @Test
    void list_fillsRatingsCategoriesAndOnlyCountsVisibleReviews() {
        Page<ProductSummaryDTO> page = list(new ProductFilter(null, null, null, false), Sort.unsorted());

        assertEquals(5, page.getTotalElements());

        ProductSummaryDTO a = find(page, phoneA);
        assertEquals(4.5, a.getAverageNote());
        assertEquals(2, a.getTotalReviews());
        assertEquals("Celulares", a.getSubCategorieName());
        assertEquals(phones.getId(), a.getSubCategorieId());
        assertEquals("Eletrônicos", a.getCategoryName());
        assertEquals(electronics.getId(), a.getCategoryId());
        assertEquals("eletronicos", a.getCategorySlug());
        assertEquals("celulares", a.getSubCategorieSlug());

        // a review oculta (nota 1) não entra na média
        ProductSummaryDTO c = find(page, coffee);
        assertEquals(5.0, c.getAverageNote());
        assertEquals(1, c.getTotalReviews());

        ProductSummaryDTO t = find(page, toaster);
        assertNull(t.getAverageNote());
        assertEquals(0, t.getTotalReviews());
    }

    @Test
    void averageNote_isRoundedToOneDecimal() {
        review(laptop, 4, ReviewStatus.VISIBLE);
        review(laptop, 4, ReviewStatus.VISIBLE);

        // 3,4,4 -> 3.666...
        assertEquals(3.7, find(list(new ProductFilter(null, null, null, false), Sort.unsorted()), laptop).getAverageNote());
    }

    @Test
    void ranking_sortsByAverageDesc_tieBrokenByTotalReviews_withoutUnrated() {
        Page<ProductSummaryDTO> page = list(new ProductFilter(null, null, null, true), Sort.by(Sort.Direction.DESC, "averageNote"));

        // cafeteira 5.0 (1) | phoneB 4.5 (4) | phoneA 4.5 (2) | laptop 3.0
        assertEquals(List.of("Cafeteira", "Smartphone B", "Smartphone A", "Notebook Pro"), names(page));
        assertEquals(4, page.getTotalElements());
    }

    @Test
    void sortByAverageNote_keepsUnratedLastInBothDirections() {
        assertEquals("Torradeira", names(list(new ProductFilter(null, null, null, false),
                Sort.by(Sort.Direction.DESC, "averageNote"))).get(4));

        List<String> asc = names(list(new ProductFilter(null, null, null, false), Sort.by(Sort.Direction.ASC, "averageNote")));
        assertEquals("Notebook Pro", asc.get(0));
        assertEquals("Torradeira", asc.get(4));
    }

    @Test
    void sortByTotalReviewsAndName() {
        List<String> byTotal = names(list(new ProductFilter(null, null, null, false), Sort.by(Sort.Direction.DESC, "totalReviews")));
        assertEquals("Smartphone B", byTotal.get(0));
        assertEquals("Smartphone A", byTotal.get(1));

        List<String> byName = names(list(new ProductFilter(null, null, null, false), Sort.by(Sort.Direction.ASC, "name")));
        assertEquals(List.of("Cafeteira", "Notebook Pro", "Smartphone A", "Smartphone B", "Torradeira"), byName);
    }

    @Test
    void filters_searchCategoryAndSubCategory() {
        assertEquals(List.of("Smartphone A", "Smartphone B"),
                names(list(new ProductFilter("SMART", null, null, false), Sort.by("name"))));
        assertEquals(List.of("Notebook Pro", "Smartphone A", "Smartphone B"),
                names(list(new ProductFilter(null, electronics.getId(), null, false), Sort.by("name"))));
        assertEquals(List.of("Cafeteira", "Torradeira"),
                names(list(new ProductFilter(null, null, kitchen.getId(), false), Sort.by("name"))));
        assertEquals(List.of("Notebook Pro"),
                names(list(new ProductFilter("note", electronics.getId(), notebooks.getId(), true), Sort.by("name"))));
    }

    @Test
    void onlyRated_countMatchesContent() {
        Page<ProductSummaryDTO> page = productRepository.findSummaries(new ProductFilter(null, null, kitchen.getId(), true),
                PageRequest.of(0, 1, Sort.by("name")));

        assertEquals(1, page.getTotalElements());
        assertEquals("Cafeteira", page.getContent().get(0).getName());
    }

    @Test
    void pagination_totalIsTheNumberOfProducts() {
        Page<ProductSummaryDTO> page = productRepository.findSummaries(new ProductFilter(null, null, null, false),
                PageRequest.of(1, 2, Sort.by("name")));

        assertEquals(5, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertEquals(List.of("Smartphone A", "Smartphone B"), names(page));
    }

    @Test
    void imageUrl_isTheFirstImage_andByIdOrSlugFindsOneProduct() {
        image(phoneA, "https://img/first.jpg");
        image(phoneA, "https://img/second.jpg");

        ProductSummaryDTO byId = productRepository.findSummaries(ProductFilter.byId(phoneA.getId()), PageRequest.of(0, 1))
                .getContent().get(0);
        assertEquals("https://img/first.jpg", byId.getImageUrl());
        assertEquals(4.5, byId.getAverageNote());

        ProductSummaryDTO bySlug = productRepository.findSummaries(ProductFilter.bySlug("smartphone-b"), PageRequest.of(0, 1))
                .getContent().get(0);
        assertEquals(phoneB.getId(), bySlug.getId());
        assertNull(bySlug.getImageUrl());

        assertTrue(productRepository.findSummaries(ProductFilter.bySlug("nope"), PageRequest.of(0, 1)).isEmpty());
    }

    // ---------- fase 3: busca sem acento, duplicados e search_name ----------

    @Test
    void search_ignoresAccentsAndCase() {
        Product cafe = product("Café Pilão Tradicional", "cafe-pilao", kitchen);

        // "cafe" acha "Café ..." e também "Cafeteira"
        var both = productRepository.findSummaries(new ProductFilter("cafe", null, null, false), PageRequest.of(0, 10));
        assertEquals(java.util.Set.of(cafe.getId(), coffee.getId()),
                both.getContent().stream().map(ProductSummaryDTO::getId).collect(java.util.stream.Collectors.toSet()));

        for (String term : java.util.List.of("CAFÉ PIL", "  pilao  trad", "Pilão")) {
            var page = productRepository.findSummaries(new ProductFilter(term, null, null, false), PageRequest.of(0, 10));
            assertEquals(java.util.List.of(cafe.getId()), page.getContent().stream().map(ProductSummaryDTO::getId).toList(), term);
        }
    }

    @Test
    void search_treatsLikeWildcardsLiterally() {
        product("Suco 100% Uva", "suco-uva", kitchen);

        assertEquals(1, productRepository.findSummaries(new ProductFilter("100%", null, null, false), PageRequest.of(0, 10))
                .getTotalElements());
        assertEquals(0, productRepository.findSummaries(new ProductFilter("_", null, null, false), PageRequest.of(0, 10))
                .getTotalElements());
    }

    @Test
    void save_keepsSearchNameInSync() {
        assertEquals("cafeteira", productRepository.findById(coffee.getId()).orElseThrow().getSearchName());

        coffee.setName("Cafeteira  Elétrica");
        productRepository.saveAndFlush(coffee);

        assertEquals("cafeteira eletrica", productRepository.findById(coffee.getId()).orElseThrow().getSearchName());
    }

    @Test
    void findDuplicates_returnsSameNormalizedNameInSameSubCategory_oldestFirst() {
        Product dupCoffee = product("CAFETEIRA ", "cafeteira-2", kitchen);
        // mesmo nome em outra subcategoria não é duplicado
        product("Cafeteira", "cafeteira-3", phones);

        java.util.List<Product> duplicates = productRepository.findDuplicates();

        assertEquals(java.util.List.of(coffee.getId(), dupCoffee.getId()), duplicates.stream().map(Product::getId).toList());
        assertTrue(productRepository.existsBySubCategorieIdAndSearchName(kitchen.getId(), "cafeteira"));
        assertFalse(productRepository.existsBySubCategorieIdAndSearchName(notebooks.getId(), "cafeteira"));
    }

    @Test
    void backfill_fillsMissingSearchNameWithoutTouchingUpdatedAt() {
        productRepository.updateSearchName(toaster.getId(), null);
        java.time.Instant updatedAt = productRepository.findById(toaster.getId()).orElseThrow().getUpdatedAt();

        int updated = new com.client.productionreview.config.SearchNameBackfill(productRepository).backfill();

        assertEquals(1, updated);
        Product reloaded = productRepository.findById(toaster.getId()).orElseThrow();
        assertEquals("torradeira", reloaded.getSearchName());
        assertEquals(updatedAt, reloaded.getUpdatedAt());
        // idempotente
        assertEquals(0, new com.client.productionreview.config.SearchNameBackfill(productRepository).backfill());
    }
}
