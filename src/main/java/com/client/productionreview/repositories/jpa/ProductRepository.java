package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends PagingAndSortingRepository<Product,Long> , JpaRepository<Product, Long>,
        ProductSummaryRepository {


    @Query("SELECT p FROM Product p WHERE p.slug = :slug")
    Optional<Product> findBySlug(final String slug);


    boolean existsBySlug(String slug);

    boolean existsBySubCategorieIdAndSearchName(Long subCategorieId, String searchName);

    /**
     * Produtos com o mesmo nome normalizado de outro da mesma subcategoria, agrupáveis pela ordem:
     * subcategoria, nome normalizado e, dentro do grupo, o mais antigo primeiro.
     */
    @Query("SELECT p FROM Product p WHERE p.searchName IS NOT NULL AND EXISTS (SELECT 1 FROM Product o "
            + "WHERE o.id <> p.id AND o.subCategorieId = p.subCategorieId AND o.searchName = p.searchName) "
            + "ORDER BY p.subCategorieId, p.searchName, p.createdAt, p.id")
    java.util.List<Product> findDuplicates();

    @Query("SELECT p.id FROM Product p WHERE p.searchName IS NULL")
    java.util.List<Long> findIdsWithoutSearchName();

    /** Atualiza só o search_name; {@code updatedAt = updatedAt} impede o ON UPDATE CURRENT_TIMESTAMP do MariaDB. */
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Product p SET p.searchName = :searchName, p.updatedAt = p.updatedAt WHERE p.id = :id")
    int updateSearchName(@Param("id") Long id, @Param("searchName") String searchName);

    @Query("SELECT p.id AS id, p.name AS name FROM Product p WHERE p.id IN :ids")
    java.util.List<IdName> findNames(@Param("ids") java.util.Collection<Long> ids);

    @Query("SELECT p.slug AS slug, p.updatedAt AS updatedAt FROM Product p ORDER BY p.id")
    java.util.List<SlugUpdated> findSlugsForSitemap();

    interface IdName {
        Long getId();

        String getName();
    }

    interface SlugUpdated {
        String getSlug();

        java.time.Instant getUpdatedAt();
    }

    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :product, '%'))")
    Page<Product> findAllByProduct(@Param("product") final String product, final Pageable pageable);

}