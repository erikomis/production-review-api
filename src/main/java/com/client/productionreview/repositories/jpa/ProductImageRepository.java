package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.jpa.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    List<ProductImage> findByProductIdOrderByIdAsc(Long productId);

    boolean existsByFilename(String filename);
}
