package com.client.productionreview.repositories.jpa;

import com.client.productionreview.model.Comentary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ComentaryRepository extends JpaRepository<Comentary, Long> {
}
