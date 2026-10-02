package com.client.productionreview.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    static final String DEFAULT_REGION = "us-east-1";

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access.name:}")
    private String accessKey;

    @Value("${minio.access.secret:}")
    private String accessSecret;

    /**
     * O endpoint respeita esquema e porta da URL (ex.: {@code http://localhost:9000} no S3 local,
     * {@code https://storage.exemplo.com} em produção). Sem credenciais, as requisições vão anônimas.
     */
    @Bean
    public MinioClient minioClient() {
        return build(url, accessKey, accessSecret);
    }

    static MinioClient build(String url, String accessKey, String accessSecret) {
        // região fixa: evita a consulta GET ?location antes de cada operação (o S3 local não precisa dela)
        MinioClient.Builder builder = MinioClient.builder().endpoint(url).region(DEFAULT_REGION);
        if (accessKey != null && !accessKey.isBlank()) {
            builder.credentials(accessKey, accessSecret == null ? "" : accessSecret);
        }
        return builder.build();
    }
}
