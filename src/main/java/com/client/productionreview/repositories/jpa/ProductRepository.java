package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends PagingAndSortingRepository<Product,Long> , JpaRepository<Product, Long> {


    @Query("SELECT p FROM Product p WHERE p.slug = :slug")
    Optional<Product> findBySlug(final String slug);


    @Query(
            value = "SELECT * FROM product p WHERE p.name = :product",
            nativeQuery = true,
            countQuery = "SELECT count(*) FROM product p WHERE p.name = :product"
    )
    Page<Product> findAllByProduct(@Param("product") final String product, final Pageable pageable);

}