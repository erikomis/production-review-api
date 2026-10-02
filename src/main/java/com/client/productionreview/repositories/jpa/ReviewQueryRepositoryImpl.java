package com.client.productionreview.repositories.jpa;

import com.client.productionreview.dtos.review.ReviewResponseDTO;
import com.client.productionreview.dtos.review.ReviewSearch;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private static final String SELECT = "SELECT new com.client.productionreview.dtos.review.ReviewResponseDTO("
            + "r.id, r.title, r.description, r.note, r.productId, r.userId, r.createdAt, p.name, p.slug, u.name, "
            + "r.status, r.moderationReason, r.moderatedAt, m.name, COUNT(h.userId)) "
            + "FROM Review r "
            + "LEFT JOIN Product p ON p.id = r.productId "
            + "LEFT JOIN User u ON u.id = r.userId "
            + "LEFT JOIN User m ON m.id = r.moderatedBy "
            + "LEFT JOIN ReviewHelpful h ON h.reviewId = r.id";

    private static final String GROUP_BY = " GROUP BY r.id, r.title, r.description, r.note, r.productId, r.userId, "
            + "r.createdAt, p.name, p.slug, u.name, r.status, r.moderationReason, r.moderatedAt, m.name";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ReviewResponseDTO> searchDetails(ReviewSearch search, Pageable pageable) {
        Map<String, Object> params = new HashMap<>();
        String where = where(search, params);

        TypedQuery<ReviewResponseDTO> query = entityManager.createQuery(
                SELECT + where + GROUP_BY + orderBy(search), ReviewResponseDTO.class);
        params.forEach(query::setParameter);
        if (pageable.isPaged()) {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
        }
        List<ReviewResponseDTO> content = query.getResultList();

        TypedQuery<Long> count = entityManager.createQuery("SELECT COUNT(r) FROM Review r" + where, Long.class);
        params.forEach(count::setParameter);

        return new PageImpl<>(new ArrayList<>(content), pageable, count.getSingleResult());
    }

    private String where(ReviewSearch search, Map<String, Object> params) {
        List<String> clauses = new ArrayList<>();
        if (search.reviewId() != null) {
            clauses.add("r.id = :reviewId");
            params.put("reviewId", search.reviewId());
        }
        if (search.productId() != null) {
            clauses.add("r.productId = :productId");
            params.put("productId", search.productId());
        }
        if (search.userId() != null) {
            clauses.add("r.userId = :userId");
            params.put("userId", search.userId());
        }
        if (search.status() != null) {
            clauses.add("r.status = :status");
            params.put("status", search.status());
        }
        if (search.note() != null) {
            clauses.add("r.note = :note");
            params.put("note", search.note());
        }
        if (search.search() != null && !search.search().isBlank()) {
            clauses.add("(LOWER(r.title) LIKE :search OR LOWER(r.description) LIKE :search)");
            params.put("search", "%" + search.search().trim().toLowerCase() + "%");
        }
        return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
    }

    private String orderBy(ReviewSearch search) {
        return switch (search.sortOrDefault()) {
            case recent -> " ORDER BY r.createdAt DESC, r.id DESC";
            case oldest -> " ORDER BY r.createdAt ASC, r.id ASC";
            case highest -> " ORDER BY r.note DESC, r.createdAt DESC, r.id DESC";
            case lowest -> " ORDER BY r.note ASC, r.createdAt DESC, r.id DESC";
            case helpful -> " ORDER BY COUNT(h.userId) DESC, r.createdAt DESC, r.id DESC";
        };
    }
}
