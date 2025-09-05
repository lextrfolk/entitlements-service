package com.app.entitlement.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak")
public class KeyClockConfigProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}
