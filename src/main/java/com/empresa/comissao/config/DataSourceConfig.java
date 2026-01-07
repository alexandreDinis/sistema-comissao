package com.empresa.comissao.config;

import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration
public class DataSourceConfig {

    @PostConstruct
    public void fixDatabaseUrl() {
        String dbUrl = System.getenv("DATABASE_URL");

        if (dbUrl != null && dbUrl.startsWith("postgresql://")) {
            String jdbcUrl = dbUrl.replace("postgresql://", "jdbc:postgresql://");
            System.setProperty("spring.datasource.url", jdbcUrl);
        }
    }
}

