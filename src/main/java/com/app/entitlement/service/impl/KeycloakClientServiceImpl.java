package com.app.entitlement.service.impl;

import com.app.entitlement.exception.KeycloakClientNotFoundException;
import com.app.entitlement.exception.UserNotFoundException;
import com.app.entitlement.service.KeycloakClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakClientServiceImpl implements KeycloakClientService {

    private final Keycloak keycloak;

    /**
     * Default realm for this application
     */
    @Value("${keycloak.realm}")
    private String defaultRealm;

    /**
     * Fetches Keycloak client by clientId.
     *
     * @param clientId Keycloak clientId
     * @return ClientRepresentation
     * @throws KeycloakClientNotFoundException if client not found
     */
    @Override
    public ClientRepresentation getClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be null or blank");
        }

        List<ClientRepresentation> clients = keycloak.realm(defaultRealm).clients().findByClientId(clientId);
        if (clients.isEmpty()) {
            log.warn("Client {} not found in realm {}", clientId, defaultRealm);
            throw new KeycloakClientNotFoundException(clientId);
        }
        log.debug("Fetched client {} in realm {}", clientId, defaultRealm);
        return clients.get(0);
    }

    /**
     * Returns client UUID for further operations.
     */
    @Override
    public String getClientIdUUID(String clientId) {
        return getClient(clientId).getId();
    }

    /**
     * Returns Keycloak ClientResource for performing actions like roles, scopes, resources.
     */
    @Override
    public ClientResource getClientResource(String clientId) {
        String clientIdUUID = getClientIdUUID(clientId);
        return keycloak.realm(defaultRealm).clients().get(clientIdUUID);
    }

    @Override
    public List<RoleRepresentation> getClientRoles(String clintId) {
        return getClientResource(clintId).roles().list();
    }

    @Override
    public List<String> getUsersWithRole(String clientId, String roleName) {
        throw new RuntimeException("Not implemented. Use getUsersWithGroupRole(String roleName) instead.");
    }

    /**
     * Fetches usernames of users assigned to a specific group with role.
     * Role is identified by name prefix in the user's group memberships.
     * For example, role "admin" matches groups like "admin|team1", "admin|team2".
     *
     * @param roleName role name prefix to search for
     * @return list of usernames assigned to the role
     */
    @Override
    public List<String> getUsersWithGroupRole(String roleName) {
        List<UserRepresentation> users = keycloak.realm(defaultRealm)
                .users()
                .search(null, 0, Integer.MAX_VALUE);

        return users.stream()
                .filter(user -> {
                    List<GroupRepresentation> groups = getUserGroups(user.getId());
                    return groups.stream()
                            .map(GroupRepresentation::getName)
                            .anyMatch(name -> name.startsWith(roleName + "|"));
                })
                .map(UserRepresentation::getUsername)
                .toList();
    }

    /**
     * Fetches all groups a user belongs to in a null-safe way.
     */
    @Override
    public List<GroupRepresentation> getUserGroups(String userId) {
        try {
            return Optional.ofNullable(
                    keycloak.realm(defaultRealm)
                            .users()
                            .get(userId)
                            .groups()
            ).orElse(Collections.emptyList());
        } catch (javax.ws.rs.NotFoundException e) {
            throw new UserNotFoundException("User not found with id: " + userId);
        }
    }
}
