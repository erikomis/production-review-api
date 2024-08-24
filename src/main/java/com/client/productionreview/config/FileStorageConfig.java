package com.client.productionreview.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileStorageConfig {
    @Bean
    public Path fileStorageLocation() {
        // Define o caminho para o diretório de armazenamento de arquivos
        return Paths.get("uploads").toAbsolutePath().normalize();
    }
}
