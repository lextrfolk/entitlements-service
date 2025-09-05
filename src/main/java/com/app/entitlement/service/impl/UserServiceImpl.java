package com.app.entitlement.service.impl;

import com.app.entitlement.config.KeyClockConfigProperties;
import com.app.entitlement.exception.UserAlreadyExistsException;
import com.app.entitlement.exception.UserCreationException;
import com.app.entitlement.exception.UserNotFoundException;
import com.app.entitlement.model.UserRequest;
import com.app.entitlement.service.PasswordService;
import com.app.entitlement.service.UserService;
import com.app.entitlement.util.PasswordUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    public final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);
    private final Keycloak keycloak;
    private final PasswordService passwordService;
    private final KeyClockConfigProperties configProperties;

    public UserServiceImpl(KeyClockConfigProperties configProperties, Keycloak keycloak,
                           PasswordService passwordService) {
        this.configProperties = configProperties;
        this.keycloak = keycloak;
        this.passwordService = passwordService;
    }

    @Override
    public String createUser(UserRequest request) {

        if(!PasswordUtils.isStrongPassword(request.getKey())){
            List<String> errors = PasswordUtils.validatePassword(request.getKey());
           throw new UserCreationException("Password does not meet strength requirements: " + String.join(", ", errors));
        }

        validateUserDoesNotExist(request.getUserName());

        UserRepresentation userRep = buildUserRepresentation(request);
        String userId = createUserInKeycloak(userRep, request.getUserName());

        // delegate password setup
        try {
            passwordService.resetUserPassword(userId, request.getKey(), false);
        } catch (Exception e) {
            logger.error("Password setup failed for user: {}", request.getUserName(), e);
            deleteUser(userId);
            throw new UserCreationException("Password setup failed for user: " + request.getUserName());
        }
        return userId;
    }

    @Override
    public List<UserRepresentation> searchByUsername(String username, boolean exact) {
        try {
            List<UserRepresentation> results =
                    keycloak.realm(configProperties.getRealm())
                            .users()
                            .searchByUsername(username, exact);

            logger.info("Found {} user(s) for username='{}', exact={}", results.size(), username, exact);
            return results;

        } catch (Exception e) {
            logger.error("Failed to search user by username='{}'", username, e);
            throw new RuntimeException("Failed to search user by username: " + e.getMessage(), e);
        }
    }


    @Override
    public void deleteUser(String userId) {
        logger.info("Deleting user with ID: {}", userId);
        try {

           UserRepresentation userRepresentation = searchByUsername(userId, true).stream().findFirst()
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

            keycloak.realm(configProperties.getRealm())
                    .users()
                    .get(userRepresentation.getId())
                    .remove();
            logger.info("User with ID: {} deleted successfully.", userId);
        } catch (NotFoundException e) {
            logger.error("User with ID: {} not found in Keycloak", userId, e);
            throw new UserNotFoundException("User not found: " + userId);
        } catch (Exception e) {
            logger.error("Failed to delete user with ID: {}", userId, e);
            throw new RuntimeException("Failed to delete user: " + userId, e);
        }
    }

    @Override
    public void setUserEnabledStatus(String userId, boolean enabled) {
        logger.info("{} user with ID: {}", enabled ? "Enabling" : "Disabling", userId);

        try {

            UserRepresentation userRepresentation = searchByUsername(userId, true).stream().findFirst()
                    .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

            UserResource userResource = keycloak.realm(configProperties.getRealm())
                    .users()
                    .get(userRepresentation.getId());

            userRepresentation.setEnabled(enabled);
            userResource.update(userRepresentation);

            logger.info("User with ID: {} {} successfully.", userId, enabled ? "enabled" : "disabled");

        } catch (NotFoundException e) {
            logger.error("User with ID: {} not found in Keycloak", userId, e);
            throw new UserNotFoundException("User not found: " + userId);
        } catch (Exception e) {
            logger.error("Failed to {} user with ID: {}", enabled ? "enable" : "disable", userId, e);
            throw new RuntimeException("Failed to " + (enabled ? "enable" : "disable") + " user: " + userId, e);
        }
    }



    /**  Validates that a user with the given username does not already exist in Keycloak.
     *   Throws UserAlreadyExistsException if a user is found.
     */
    private void validateUserDoesNotExist(String username) {
        List<UserRepresentation> users = searchByUsername(username, true);

        boolean exists = users.stream()
                .anyMatch(u -> username.equalsIgnoreCase(u.getUsername()));

        if (exists) {
            throw new UserAlreadyExistsException("Username already exists: " + username);
        }
    }

    private UserRepresentation buildUserRepresentation(UserRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUserName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());

        // TODO : Configure email settings for email verification
        user.setEmailVerified(true);
        user.setEnabled(true);
        return user;
    }

    private String createUserInKeycloak(UserRepresentation user, String username) {
        try (Response response = keycloak.realm(configProperties.getRealm()).users().create(user)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                String errorBody = response.readEntity(String.class);
                throw new UserCreationException("User creation failed: " + errorBody);
            }
            return response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        }
    }
}
