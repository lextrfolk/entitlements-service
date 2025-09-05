package com.app.entitlement.controller;

import com.app.entitlement.constants.CommonConstants;
import com.app.entitlement.dto.ApiResponse;
import com.app.entitlement.model.RoleRequest;
import com.app.entitlement.model.UserRequest;
import com.app.entitlement.model.UserGroupAssignmentRequest;
import com.app.entitlement.model.UserGroupAssignmentResponse;
import com.app.entitlement.service.KeycloakAuthorizationProvisionerService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.app.entitlement.constants.CommonConstants.SAVE_METADATA_API;

/**
 * REST controller for handling entitlement provisioning operations
 * such as user signup, role creation, group assignments, and metadata provisioning.
 */
@RestController
@RequestMapping(CommonConstants.ENTITLEMENT_BASE_PATH)
public class ProvisioningController {

    private static final Logger logger = LoggerFactory.getLogger(ProvisioningController.class);
    private final KeycloakAuthorizationProvisionerService provisionerService;

    public ProvisioningController(KeycloakAuthorizationProvisionerService keycloakAuthorizationProvisionerService) {
        this.provisionerService = keycloakAuthorizationProvisionerService;
    }

    /**
     * Provisions entitlement metadata such as forms and schedules.
     *
     * @param formScheduleMap a map containing form-schedule relationships
     * @return a success response indicating metadata provisioning has started
     */
    @PostMapping(SAVE_METADATA_API)
    public ResponseEntity<ApiResponse<String>> createMetadata(@RequestBody Map<String, List<String>> formScheduleMap) {
        logger.info("Received request to provision metadata with {} entries.", formScheduleMap.size());
        provisionerService.provisionResources(formScheduleMap);
        logger.debug("Metadata provisioning initiated successfully.");
        return ResponseEntity.ok(ApiResponse.success("Metadata provisioning started.", null));
    }

    /**
     * Assigns a user to a group.
     *
     * @param userId  the ID of the user
     * @param request the group assignment request
     * @return a success response if the user is assigned to the group
     */
    @PostMapping("/users/{userId}/groups")
    public ResponseEntity<ApiResponse<String>> assignUserToGroup(
            @PathVariable String userId,
            @Valid @RequestBody UserGroupAssignmentRequest request) {

        logger.info("Assigning user [{}] to group(s): {}", userId, request.getData());
        provisionerService.assignUserToGroup(request);
        logger.debug("User [{}] assigned to group(s) successfully.", userId);

        return ResponseEntity.ok(ApiResponse.success("User assigned to group successfully.", null));
    }

    /**
     * Fetches groups associated with a given user.
     *
     * @param userId the ID of the user
     * @return a response containing the list of groups assigned to the user
     */
    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<ApiResponse<UserGroupAssignmentResponse>> getUserGroups(@PathVariable String userId) {
        logger.info("Fetching groups for user [{}].", userId);
        UserGroupAssignmentResponse responseRequest = provisionerService.getUserEntl(userId);
        return ResponseEntity.ok(ApiResponse.success("User groups fetched successfully.", responseRequest));
    }





}
