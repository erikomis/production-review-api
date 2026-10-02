package com.client.productionreview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

import java.util.TimeZone;

@SpringBootApplication
@EnableCaching
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class ProductionReviewApplication {

    public static final String UTC = "UTC";

    public static void main(String[] args) {
        // datas gravadas e devolvidas sempre em UTC, independentemente do fuso da máquina
        TimeZone.setDefault(TimeZone.getTimeZone(UTC));
        SpringApplication.run(ProductionReviewApplication.class, args);
    }

}
