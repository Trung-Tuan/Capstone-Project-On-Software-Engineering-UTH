package com.uth.capstone.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Capstone Project API - UTH")
                        .version("1.0.0")
                        .description("API Documentation for Capstone Project on Software Engineering (UTH)")
                        .contact(new Contact()
                                .name("UTH Software Engineering")
                                .email("contact@uth.edu.vn"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }
}

