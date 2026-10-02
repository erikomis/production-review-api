package com.client.productionreview.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebMvcConfig(@Value("${app.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedHeaders("*")
                .allowCredentials(true)
                .allowedMethods("GET", "POST", "PUT", "DELETE","PATCH" ,"OPTIONS", "HEAD")
                // headers que os fronts precisam ler: tempo de espera do 429 e nome do arquivo CSV
                .exposedHeaders("Retry-After", "Content-Disposition");
    }


}
