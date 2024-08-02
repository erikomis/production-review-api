package com.client.productionreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProductionReviewApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductionReviewApplication.class, args);
    }

}
