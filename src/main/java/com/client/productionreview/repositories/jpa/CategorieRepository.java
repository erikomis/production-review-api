package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.Categorie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie , Long> {

    Optional<Categorie> findByName(String categorieName);
}
