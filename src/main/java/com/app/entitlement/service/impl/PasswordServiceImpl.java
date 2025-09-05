package com.app.entitlement.service.impl;

import com.app.entitlement.config.KeyClockConfigProperties;
import com.app.entitlement.exception.InvalidPasswordException;
import com.app.entitlement.exception.UserNotFoundException;
import com.app.entitlement.service.PasswordService;
import com.app.entitlement.util.PasswordUtils;
import lombok.AllArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class PasswordServiceImpl implements PasswordService  {

    public final Logger logger = LoggerFactory.getLogger(PasswordServiceImpl.class);

    private final Keycloak keycloak;
    private final KeyClockConfigProperties config;

    @Override
    public void resetUserPassword(String userId, String newPassword, boolean temporary) {

        // Validate password strength
        List<String> errors = PasswordUtils.validatePassword(newPassword);
        if (!errors.isEmpty()) {
            throw new InvalidPasswordException(
                    "Password reset failed. New password does not meet strength requirements: "
                            + String.join(", ", errors));
        }


        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(newPassword);
        cred.setTemporary(temporary);

        keycloak.realm(config.getRealm())
                .users()
                .get(userId)
                .resetPassword(cred);

        logger.info("Password updated for userId={}, temporary={}", userId, temporary);
    }

    @Override
    public void changePassword(String username, String currentPassword, String newPassword) {
        List<UserRepresentation> users = keycloak.realm(config.getRealm())
                .users()
                .search(username, true);

        if (users.isEmpty()) {
            throw new UserNotFoundException("User not found: " + username);
        }

        // Validate via user authentication
        validateCurrentPassword(username, currentPassword);

        resetUserPassword(users.get(0).getId(), newPassword, false);

        // TODO : Enable password history check in Keycloak and remove this comment
        // TODO : New password should be different from current password.
        // Note : Password history enforcement must be configured in Keycloak realm settings.

    }
    /**
     * Sends a reset password email to the user via Keycloak.
     */
    @Override
    public void sendResetPasswordEmail(String userId) {
        keycloak.realm(config.getRealm())
                .users()
                .get(userId)
                .executeActionsEmail(List.of("UPDATE_PASSWORD"));

        logger.info("Reset password email sent for userId={}", userId);
    }

    /**
     * Requires the user to update their password on next login.
     */
    @Override
    public void requirePasswordUpdate(String userId) {
        // Enforce password change on next login without sending email
        UserResource userResource = keycloak.realm(config.getRealm()).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        // Add "UPDATE_PASSWORD" to required actions if not already present
        List<String> actions = user.getRequiredActions();
        if (actions == null) {
            actions = new ArrayList<>();
        }
        if (!actions.contains("UPDATE_PASSWORD")) {
            actions.add("UPDATE_PASSWORD");
            user.setRequiredActions(actions);
            userResource.update(user);
        }

        logger.info("Password update required on next login for userId={}", userId);
    }

    public void validateCurrentPassword(String username, String currentPassword) {
        try (Keycloak userKeycloak = KeycloakBuilder.builder()
                .serverUrl(config.getServerUrl())
                .realm(config.getRealm())
                .username(username)
                .password(currentPassword)
                .clientId(config.getClientId())
                .clientSecret(config.getClientSecret())
                .grantType(OAuth2Constants.PASSWORD)
                .build()) {

            // Attempt to fetch token (forces authentication)
            userKeycloak.tokenManager().getAccessToken();

        } catch (javax.ws.rs.NotAuthorizedException ex) {
            throw new InvalidPasswordException("Current password is incorrect");
        } catch (javax.ws.rs.ForbiddenException ex) {
            throw new RuntimeException("User exists but cannot login (disabled or restricted)", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to validate current password due to server error", ex);
        }
    }


}
