package com.javaweb.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI realEstateOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real Estate Management API")
                        .description("API documentation for the Real Estate Management backend")
                        .version("v1")
                        .contact(new Contact().name("Real Estate Management Team"))
                        .license(new License().name("Proprietary")));
    }
}
