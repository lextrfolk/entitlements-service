package com.app.entitlement.controller;

import com.app.entitlement.model.UserGroupAssignmentRequest;
import com.app.entitlement.service.KeycloakAuthorizationProvisionerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/entitlements")
public class ProvisioningController {

    @Autowired
    private KeycloakAuthorizationProvisionerService keycloakAuthorizationProvisionerService;

    @PostMapping("/createMetadata")
    public ResponseEntity<String> provision(@RequestBody Map<String, List<String>> formScheduleMap) {
        keycloakAuthorizationProvisionerService.provisionResources(formScheduleMap);
        return ResponseEntity.ok("Provisioning started.");
    }

    @PostMapping("/assignUserToGroup")
    public ResponseEntity<String> assignUserToGroup(@RequestBody UserGroupAssignmentRequest request) {

        try {
            keycloakAuthorizationProvisionerService.assignUserToGroup(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error during group assignment: " + e.getMessage());
        }

        return ResponseEntity.ok("Provisioning started.");
    }

    @GetMapping("/user-groups")
    public ResponseEntity<UserGroupAssignmentRequest> getUserGroups(@RequestParam String userId) {
        UserGroupAssignmentRequest response = keycloakAuthorizationProvisionerService.getUserEntl(userId);
        return ResponseEntity.ok(response);
    }



}
