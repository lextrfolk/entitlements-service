package com.app.entitlement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.RoleRepresentation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {
    private String id;
    private String name;
    private String description;
    private boolean clientRole;

    public static RoleDto mapToDTO(RoleRepresentation role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getClientRole() != null && role.getClientRole());
}
}
