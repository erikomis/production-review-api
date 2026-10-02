package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.product.ProductFilter;
import com.client.productionreview.dtos.product.ProductSortProperty;
import com.client.productionreview.dtos.product.ProductSuggestionDTO;
import com.client.productionreview.dtos.product.ProductSummaryDTO;
import com.client.productionreview.model.jpa.ProductImage;
import com.client.productionreview.model.jpa.ReviewStatus;
import com.client.productionreview.utils.TextNormalizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductSummaryRepositoryImpl implements ProductSummaryRepository {

    private static final String SELECT = "SELECT new com.client.productionreview.dtos.product.ProductSummaryDTO("
            + "p.id, p.name, p.description, p.slug, s.id, s.name, s.slug, c.id, c.name, c.slug, p.createdAt, AVG(r.note), COUNT(r.id)) ";

    private static final String FROM = "FROM Product p "
            + "JOIN SubCategory s ON s.id = p.subCategorieId "
            + "JOIN Category c ON c.id = s.categorieId ";

    private static final String GROUP_BY = " GROUP BY p.id, p.name, p.description, p.slug, s.id, s.name, s.slug, c.id, c.name, c.slug, p.createdAt";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ProductSummaryDTO> findSummaries(ProductFilter filter, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        String where = where(filter, params);

        String jpql = SELECT + FROM
                + "LEFT JOIN Review r ON r.productId = p.id AND r.status = :visible"
                + where + GROUP_BY + orderBy(pageable.getSort());

        TypedQuery<ProductSummaryDTO> query = entityManager.createQuery(jpql, ProductSummaryDTO.class);
        query.setParameter("visible", ReviewStatus.VISIBLE);
        params.forEach(query::setParameter);
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<ProductSummaryDTO> content = query.getResultList();
        fillImages(content);

        TypedQuery<Long> count = entityManager.createQuery("SELECT COUNT(p) " + FROM + where, Long.class);
        params.forEach(count::setParameter);
        if (filter.onlyRated()) {
            count.setParameter("visible", ReviewStatus.VISIBLE);
        }

        return new PageImpl<>(new ArrayList<>(content), pageable, count.getSingleResult());
    }

    @Override
    public List<ProductSuggestionDTO> suggest(String normalizedTerm, int limit) {
        String escaped = TextNormalizer.escapeLike(normalizedTerm);
        List<ProductSuggestionDTO> content = entityManager.createQuery(
                        "SELECT new com.client.productionreview.dtos.product.ProductSuggestionDTO(p.id, p.name, p.slug, c.name) "
                                + FROM + "WHERE p.searchName LIKE :contains ESCAPE '!' "
                                + "ORDER BY CASE WHEN p.searchName LIKE :prefix ESCAPE '!' THEN 0 ELSE 1 END, p.searchName, p.id",
                        ProductSuggestionDTO.class)
                .setParameter("contains", "%" + escaped + "%")
                .setParameter("prefix", escaped + "%")
                .setMaxResults(limit)
                .getResultList();
        Map<Long, String> images = firstImages(content.stream().map(ProductSuggestionDTO::getId).toList());
        content.forEach(item -> item.setImageUrl(images.get(item.getId())));
        return new ArrayList<>(content);
    }

    private String where(ProductFilter filter, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();
        String search = TextNormalizer.normalize(filter.search());
        if (search != null && !search.isEmpty()) {
            // busca sem acento e sem diferenciar maiúsculas: "cafe" acha "Café"
            clauses.add("p.searchName LIKE :search ESCAPE '!'");
            params.put("search", "%" + TextNormalizer.escapeLike(search) + "%");
        }
        if (filter.categoryId() != null) {
            clauses.add("c.id = :categoryId");
            params.put("categoryId", filter.categoryId());
        }
        if (filter.subCategorieId() != null) {
            clauses.add("s.id = :subCategorieId");
            params.put("subCategorieId", filter.subCategorieId());
        }
        if (filter.productId() != null) {
            clauses.add("p.id = :productId");
            params.put("productId", filter.productId());
        }
        if (filter.slug() != null) {
            clauses.add("p.slug = :slug");
            params.put("slug", filter.slug());
        }
        if (filter.followedBy() != null) {
            clauses.add("EXISTS (SELECT 1 FROM ProductFollow f WHERE f.productId = p.id AND f.userId = :followedBy)");
            params.put("followedBy", filter.followedBy());
        }
        if (filter.onlyRated()) {
            clauses.add("EXISTS (SELECT 1 FROM Review rv WHERE rv.productId = p.id AND rv.status = :visible)");
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String orderBy(Sort sort) {
        List<String> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String direction = order.isAscending() ? "ASC" : "DESC";
            switch (ProductSortProperty.from(order.getProperty())) {
                case name -> orders.add("p.name " + direction);
                case createdAt -> orders.add("p.createdAt " + direction);
                case totalReviews -> orders.add("COUNT(r.id) " + direction);
                case averageNote -> {
                    // sem nota sempre no fim, em qualquer direção
                    orders.add("CASE WHEN COUNT(r.id) = 0 THEN 1 ELSE 0 END ASC");
                    orders.add("ROUND(AVG(r.note), 1) " + direction);
                    orders.add("COUNT(r.id) DESC");
                }
            }
        }
        orders.add("p.id ASC");
        return " ORDER BY " + String.join(", ", orders);
    }

    /** Primeira imagem (menor id) de cada produto da página, numa única consulta. */
    private void fillImages(List<ProductSummaryDTO> content) {
        if (content.isEmpty()) {
            return;
        }
        Map<Long, String> firstImage = firstImages(content.stream().map(ProductSummaryDTO::getId).toList());
        content.forEach(product -> product.setImageUrl(firstImage.get(product.getId())));
    }

    private Map<Long, String> firstImages(List<Long> ids) {
        Map<Long, String> firstImage = new HashMap<>();
        if (ids.isEmpty()) {
            return firstImage;
        }
        entityManager.createQuery("SELECT i FROM ProductImage i WHERE i.productId IN :ids ORDER BY i.id", ProductImage.class)
                .setParameter("ids", ids)
                .getResultList()
                .forEach(image -> firstImage.putIfAbsent(image.getProductId(), image.getUrlImage()));
        return firstImage;
    }
}
