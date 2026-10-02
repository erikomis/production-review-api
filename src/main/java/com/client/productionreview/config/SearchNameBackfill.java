package com.client.productionreview.config;

import com.client.productionreview.model.jpa.Product;
import com.client.productionreview.repositories.jpa.ProductRepository;
import com.client.productionreview.utils.TextNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Preenche {@code product.search_name} dos produtos antigos (coluna criada pela V4) na subida da aplicação.
 *
 * <p>Por que aqui e não na migração: a remoção de acentos precisa ser idêntica à do save
 * ({@link TextNormalizer}) e não existe função SQL portável entre MariaDB e H2 para isso.
 * É idempotente: só toca produtos com {@code search_name} nulo e não altera {@code updated_at}.</p>
 */
@Slf4j
@Component
public class SearchNameBackfill implements ApplicationRunner {

    private final ProductRepository productRepository;

    public SearchNameBackfill(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            int updated = backfill();
            if (updated > 0) {
                log.info("search_name preenchido em {} produtos", updated);
            }
        } catch (Exception e) {
            log.warn("Falha ao preencher search_name: {}", e.getMessage());
        }
    }

    public int backfill() {
        List<Long> ids = productRepository.findIdsWithoutSearchName();
        int updated = 0;
        for (Long id : ids) {
            Product product = productRepository.findById(id).orElse(null);
            if (product != null && product.getName() != null) {
                updated += productRepository.updateSearchName(id, TextNormalizer.normalize(product.getName()));
            }
        }
        return updated;
    }
}
