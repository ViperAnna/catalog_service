package ru.klimovich.catalog_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog Service API",
                version = "1.0",
                description = "Documentation of the catalog service"
        )
)
public class OpenApiConfig {
}
