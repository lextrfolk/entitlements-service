package com.app.entitlement.config;


import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
	@Bean
	public OpenAPI apiInfo() {
		return new OpenAPI()
				.info(new Info()
						.title("Entitlements API")
						.description("API documentation for Entitlements service")
						.version("v0.0.1"));
	}
}