package com.app.entitlement.config;

import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KeyClockConfigProperties.class)
public class KeycloakConfig {

    private final Logger logger = LoggerFactory.getLogger(KeycloakConfig.class);
    private final KeyClockConfigProperties properties;

    /**
     * Creates a Keycloak admin client using client credentials.
     * This bean can be injected into services instead of manually creating it.
     */
    @Bean
    public Keycloak keycloakAdminClient() {

        logger.debug("Registering Keycloak admin client with server URL: {}, realm: {}, client ID: {}", properties.getServerUrl(), properties.getRealm(), properties.getClientId());

        return KeycloakBuilder.builder()
                .serverUrl(properties.getServerUrl())
                .realm(properties.getRealm())
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }
}
