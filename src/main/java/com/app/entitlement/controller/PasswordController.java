package com.app.entitlement.controller;

import com.app.entitlement.service.PasswordService;
import com.app.entitlement.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.app.entitlement.constants.CommonConstants.PASSWORD_BASE_PATH;

@RestController
@RequestMapping(PASSWORD_BASE_PATH)
@RequiredArgsConstructor
public class PasswordController {
    public final Logger logger = LoggerFactory.getLogger(PasswordController.class);
    private final PasswordService passwordService;

    /**
     * Admin resets a user's password.
     */
    @PostMapping("/{userId}/reset")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(@PathVariable String userId,
                                                               @RequestParam String newPassword,
                                                               @RequestParam(defaultValue = "false") boolean temporary) {
        logger.info("Resetting password for userId={}, temporary={}", userId, temporary);
        passwordService.resetUserPassword(userId, newPassword, temporary);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    /**
     * User changes their own password by validating current password.
     */
    @PostMapping("/{username}/change")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable String username,
                                                            @RequestParam String currentPassword,
                                                            @RequestParam String newPassword) {
        logger.info("User {} is attempting to change password", username);
        passwordService.changePassword(username, currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    /**
     * Admin triggers Keycloak to send a reset email.
     */
    @PostMapping("/{userId}/reset-email")
    public ResponseEntity<ApiResponse<Void>> sendResetPasswordEmail(@PathVariable String userId) {
        logger.info("Sending reset password email to userId={}", userId);
        passwordService.sendResetPasswordEmail(userId);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", null));
    }

    /**
     * Admin requires a user to update password on next login.
     */
    @PostMapping("/{userId}/require-update")
    public ResponseEntity<ApiResponse<Void>> requirePasswordUpdate(@PathVariable String userId) {
        logger.info("Requiring password update for userId={}", userId);
        passwordService.requirePasswordUpdate(userId);
        return ResponseEntity.ok(ApiResponse.success("Password update required for next login", null));
    }
}
