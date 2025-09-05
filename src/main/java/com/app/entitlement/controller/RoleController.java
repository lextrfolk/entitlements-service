package com.app.entitlement.controller;

import com.app.entitlement.dto.ApiResponse;
import com.app.entitlement.dto.RoleDto;
import com.app.entitlement.model.RoleRequest;
import com.app.entitlement.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.app.entitlement.constants.CommonConstants.ROLES_BASE_PATH;

@RestController
@RequestMapping(ROLES_BASE_PATH)
@RequiredArgsConstructor

public class RoleController {

    public final Logger logger = LoggerFactory.getLogger(RoleController.class);
    private final RoleService roleService;

    /**
     * List all roles
     */
    @GetMapping
    public ResponseEntity<List<RoleDto>> getRoles() {
        List<RoleDto> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get role details
     */
    @GetMapping("/{roleName}")
    public ResponseEntity<RoleDto> getRole(@PathVariable String roleName) {
        RoleDto role = roleService.getRoleByName(roleName);
        return ResponseEntity.ok(role);
    }

    /**
     * Creates new roles in the entitlement system.
     *
     * @param roles the list of roles with names and descriptions
     * @return a success response when the roles are created
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRoles(@RequestBody List<RoleRequest> roles) {
        Map<String, Object> response = roleService.createRoles(roles);
        return ResponseEntity.ok(ApiResponse.success("Role creation completed", response));
    }


    /**
     * Delete a role
     */
    @DeleteMapping("/{roleName}")
    public ResponseEntity<Void> deleteRole(@PathVariable String roleName) {
        roleService.deleteRole(roleName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roleName}/users")
    public ResponseEntity<List<String>> getUsersForRole(@PathVariable String roleName) {
        List<String> users = roleService.getUsersForRole(roleName);
        return ResponseEntity.ok(users);
    }

}
