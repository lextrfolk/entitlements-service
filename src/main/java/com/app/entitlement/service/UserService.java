package com.app.entitlement.service;

import com.app.entitlement.model.UserRequest;
import jakarta.validation.Valid;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface UserService {
    String createUser(@Valid UserRequest request);

    List<UserRepresentation> searchByUsername(String username, boolean exact);

    void deleteUser(String userId);

    void setUserEnabledStatus(String userId, boolean enabled);
}
