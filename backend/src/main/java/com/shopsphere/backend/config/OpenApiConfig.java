package com.shopsphere.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI configuration. Phase 1: only basic API info.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI shopSphereOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopSphere API")
                        .description("ShopSphere backend API")
                        .version("v0.0.1"));
    }
}