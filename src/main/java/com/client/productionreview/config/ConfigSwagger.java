package com.client.productionreview.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class ConfigSwagger {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Production Review API")
                        .description("Production Review API implemented using springdoc-openapi and OpenAPI 3.")
                        .version("v.1.0.0"));
    }
}
