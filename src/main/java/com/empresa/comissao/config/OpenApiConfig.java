package com.empresa.comissao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Controle de Comissões API")
                        .version("1.0")
                        .description("API para gerenciamento de faturamentos, adiantamentos e cálculo de comissões.")
                        .contact(new Contact()
                                .name("Suporte")
                                .email("suporte@empresa.com")));
    }
}
