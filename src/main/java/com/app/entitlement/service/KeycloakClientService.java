package com.app.entitlement.service;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;

public interface KeycloakClientService {
    ClientRepresentation getClient(String clientId);

    String getClientIdUUID(String clientId);

    ClientResource getClientResource(String clientId);

    List<RoleRepresentation> getClientRoles(String clientId);

    List<String> getUsersWithRole(String clientId, String roleName);

    List<String> getUsersWithGroupRole(String roleName);

    List<GroupRepresentation> getUserGroups(String userId);
}
