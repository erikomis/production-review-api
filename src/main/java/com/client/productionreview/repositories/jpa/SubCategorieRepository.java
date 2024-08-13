package com.client.productionreview.repositories.jpa;
import com.client.productionreview.model.SubCategorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubCategorieRepository   extends JpaRepository<SubCategorie, Long> {

    Optional<SubCategorie> findByName(String subCategorieName);
}
