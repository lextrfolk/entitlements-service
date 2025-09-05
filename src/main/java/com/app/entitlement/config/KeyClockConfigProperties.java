package com.app.entitlement.config;


import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "keycloak")
public class KeyClockConfigProperties {
    private String serverUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}
