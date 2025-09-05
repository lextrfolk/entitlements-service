package com.app.entitlement.exception;

public class KeycloakClientNotFoundException extends RuntimeException {
    public KeycloakClientNotFoundException(String clientId) {
        super("No client found with clientId: " + clientId);
    }
}
