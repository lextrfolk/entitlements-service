package com.app.entitlement.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the Entitlements service.
 * Provides API documentation metadata such as title, description, and version.
 */
@Configuration
public class OpenApiConfig {

    private static final String API_TITLE = "Entitlements API";
    private static final String API_DESCRIPTION = "API documentation for Entitlements service";
    private static final String API_VERSION = "v0.0.1";

    /**
     * Configures OpenAPI metadata.
     *
     * @return configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(API_TITLE)
                        .description(API_DESCRIPTION)
                        .version(API_VERSION));
    }
}