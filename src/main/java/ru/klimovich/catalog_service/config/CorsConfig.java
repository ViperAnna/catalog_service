package ru.klimovich.catalog_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.mapping.path}")
    private String mappingPath;

    @Value("${cors.allowed.origins}")
    private String allowedOrigins;

    @Value("${cors.allowed.methods}")
    private String allowedMethod;

    @Value("${cors.allowed.headers}")
    private String allowedHeaders;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] methods = allowedMethod.split(",");
        registry.addMapping(mappingPath)
                .allowedOrigins(allowedOrigins)
                .allowedMethods(methods)
                .allowedHeaders(allowedHeaders);
    }
}
