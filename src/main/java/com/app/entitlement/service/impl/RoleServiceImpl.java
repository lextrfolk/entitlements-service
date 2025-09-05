package com.app.entitlement.service.impl;

import com.app.entitlement.config.KeyClockConfigProperties;
import com.app.entitlement.dto.RoleDto;
import com.app.entitlement.exception.RoleAssignedException;
import com.app.entitlement.exception.RoleNotFoundException;
import com.app.entitlement.model.RoleRequest;
import com.app.entitlement.service.KeycloakClientService;
import com.app.entitlement.service.RoleService;
import lombok.AllArgsConstructor;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoleServiceImpl implements RoleService {

    public final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final KeyClockConfigProperties config;
    private final KeycloakClientService keycloakClientService;

    @Override
    public Map<String, Object> createRoles(List<RoleRequest> roles) {
        ClientResource clientResource = keycloakClientService.getClientResource(config.getClientId());
        List<RoleRepresentation> existingRoles = clientResource.roles().list();

        Set<String> existingRoleNames = existingRoles.stream()
                .map(RoleRepresentation::getName)
                .collect(Collectors.toSet());

        List<String> created = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (RoleRequest roleDTO : roles) {
            if (existingRoleNames.contains(roleDTO.getName())) {
                skipped.add(roleDTO.getName());
                continue;
            }

            // Create role
            RoleRepresentation role = new RoleRepresentation();
            role.setName(roleDTO.getName());
            role.setDescription(roleDTO.getDescription());
            try {
                clientResource.roles().create(role);
                created.add(roleDTO.getName());
            } catch (Exception e) {
                logger.error("Failed to create role {}: {}", roleDTO.getName(), e.getMessage(), e);
                skipped.add(roleDTO.getName());
            }
        }

        Map<String, Object> response = Map.of(
                "created", created,
                "skipped", skipped
        );
        logger.info("Role creation summary: {}", response);

        return response;
    }

    @Override
    public RoleDto getRoleByName(String roleName) {

        RoleRepresentation roleRepresentation = keycloakClientService.getClientRoles(config.getClientId())
                .stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));
        logger.info("Fetched role: {}", roleRepresentation.getName());
        return RoleDto.mapToDTO(roleRepresentation);
    }

    @Override
    public List<RoleDto> getAllRoles() {
        List<RoleDto> roles = keycloakClientService.getClientRoles(config.getClientId()).stream()
                .map(RoleDto::mapToDTO)
                .toList();
        logger.info("Fetched all roles: {}", roles);
        return roles;
    }

    @Override
    public void deleteRole(String roleName) {
        logger.info("Deleting role: {}", roleName);

        // Fetch all roles
        List<RoleRepresentation> roles = keycloakClientService.getClientRoles(config.getClientId());

        // Check if role exists
        roles.stream()
                .filter(r -> r.getName().equals(roleName))
                .findFirst()
                .orElseThrow(() -> new RoleNotFoundException("Role not found: " + roleName));

        // Check if role is assigned to any user
        List<String> usersWithGroupRole = keycloakClientService.getUsersWithGroupRole(roleName);
        if (!usersWithGroupRole.isEmpty()) {
            throw new RoleAssignedException("Role '" + roleName + "' is assigned to users and cannot be deleted.");
        }

        // Delete role
        keycloakClientService.getClientResource(config.getClientId())
                .roles()
                .deleteRole(roleName);

        logger.info("Deleted role: {}", roleName);
    }


    public List<String> getUsersForRole(String roleName) {
        // Uses KeycloakClientService.getUsersWithRole()
        return keycloakClientService.getUsersWithRole(config.getClientId(), roleName);
    }





}