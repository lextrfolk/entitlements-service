package com.app.entitlement.service;

import com.app.entitlement.dto.RoleDto;
import com.app.entitlement.model.RoleRequest;

import java.util.List;
import java.util.Map;

public interface RoleService {
    Map<String, Object> createRoles(List<RoleRequest> roles);

    RoleDto getRoleByName(String roleName);

    List<RoleDto> getAllRoles();

    void deleteRole(String roleName);

    List<String> getUsersForRole(String roleName);
}