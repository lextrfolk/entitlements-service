package com.app.entitlement.controller;

import com.app.entitlement.dto.ApiResponse;
import com.app.entitlement.dto.UserSummaryDto;
import com.app.entitlement.model.UserRequest;
import com.app.entitlement.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import static com.app.entitlement.constants.CommonConstants.USER_BASE_PATH;

@RestController
@RequestMapping(USER_BASE_PATH)
@AllArgsConstructor
public class UserController {

    public final Logger logger = LoggerFactory.getLogger(UserController.class);

    private UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createUser(@Valid @RequestBody UserRequest request) {
        logger.info("Creating new user with username: {}", request.getUserName());

        // Service should return the Keycloak userId
        String userId = userService.createUser(request);

        logger.info("User [{}] created successfully with ID={}", request.getUserName(), userId);

        return ResponseEntity
                .created(URI.create("/api/users/" + userId))
                .body(ApiResponse.success("User created successfully.", userId));
    }


    /**
     * Search user by username.
     */
    @GetMapping("/search/user")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> findUserByUsername(
            @RequestParam String username,
            @RequestParam(defaultValue = "true", required = false) boolean exact) {

        logger.info("Searching for user by username='{}', exact={}", username, exact);

        List<UserSummaryDto> users = userService.searchByUsername(username, exact).stream()
                .map(UserSummaryDto::from)
                .toList();

        if (users.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure("User not found with username: " + username, null));
        }

        return ResponseEntity.ok(ApiResponse.success("Users found.", users));
    }



    /**
     * Delete a user permanently.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String userId) {
        logger.info("Deleting user with ID={}", userId);

        userService.deleteUser(userId);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully.", null));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String userId,
            @RequestParam boolean enabled) {

        logger.info("{} user with ID={}", enabled ? "Enabling" : "Disabling", userId);
        userService.setUserEnabledStatus(userId, enabled);

        return ResponseEntity.ok(ApiResponse.success(
                "User " + (enabled ? "enabled" : "disabled") + " successfully.", null));
    }

}
