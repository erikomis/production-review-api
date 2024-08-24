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
public interface ProductRepository extends PagingAndSortingRepository<Product,Long> , JpaRepository<Product, Long> {


    @Query("SELECT p FROM Product p WHERE p.slug = :slug")
    Optional<Product> findBySlug(final String slug);


    @Query(
            value = "SELECT * FROM Product p WHERE p.name LIKE :product",
            countQuery = "SELECT count(*) FROM product",
            nativeQuery = true
    )
    Page<Product> findAllByProduct(@Param("product") final String product, final Pageable pageable);

}