package com.app.entitlement.service;

import com.app.entitlement.config.KeyClockConfigProperties;
import com.app.entitlement.exception.KeycloakClientNotFoundException;
import com.app.entitlement.exception.UserAlreadyExistsException;
import com.app.entitlement.exception.UserCreationException;
import com.app.entitlement.exception.UserNotFoundException;
import com.app.entitlement.model.*;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@EnableConfigurationProperties(KeyClockConfigProperties.class)
public class KeycloakAuthorizationProvisionerService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.clientSecret}")
    private String clientSecret;

    private Keycloak keycloak;

    private final RestTemplate restTemplate = new RestTemplate();

    public KeycloakAuthorizationProvisionerService(Keycloak keycloak){
        this.keycloak = keycloak;
    }

    private String getAdminToken() {
        String url = serverUrl + "/realms/entlrealm/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> params = Map.of(
                "client_id", clientId,
                "client_secret",clientSecret,
                "grant_type", "client_credentials"
        );

        HttpEntity<String> request = new HttpEntity<>(buildForm(params), headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            return (String) response.getBody().get("access_token");
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            System.err.println("Keycloak Token Error: " + e.getStatusCode());
            System.err.println("Response Body:\n" + e.getResponseBodyAsString());
            throw e;
        }
    }
    private String buildForm(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        params.forEach(builder::queryParam);
        return builder.build().getQuery();
    }

    public void provisionResources(Map<String, List<String>> formScheduleMap) {
        ClientRepresentation client = getClient(clientId);
        String clientUUID = client.getId();

        List<RoleRepresentation> clientRoles = getClientRoles(clientUUID);
        for (RoleRepresentation role : clientRoles) {
            String roleName = role.getName();
            for (Map.Entry<String, List<String>> entry : formScheduleMap.entrySet()) {
                String form = entry.getKey();
                List<String> schedules = entry.getValue();

                createResource(clientUUID, form, schedules);

                for (String schedule : schedules) {
                    createScope(clientUUID, schedule);
                    String groupName = roleName+"|"+form + "|" + schedule;
                    String groupId = createGroupIfNotExists(groupName);
                    createGroupPolicy(clientUUID, groupName + "|policy", groupId);
                    createPermission(clientUUID, groupName + "|permission", form, schedule, groupName + "|policy");
                }
            }
        }

    }

    private List<RoleRepresentation> getClientRoles(String clientUUID) {
        return keycloak.realm(realm)
                .clients()
                .get(clientUUID)
                .roles()
                .list();
    }

    private ClientRepresentation getClient(String clientId) {
        List<ClientRepresentation> clientRepresentations = keycloak.realm(realm).clients().findByClientId(clientId);
        if (clientRepresentations.isEmpty())
            throw new KeycloakClientNotFoundException(clientId);
        return clientRepresentations.get(0);
    }

    private void createResource(String clientUUID, String form, List<String> scopes) {
        ResourceRepresentation resource = new ResourceRepresentation();
        resource.setName(form);
        resource.setType("form");
        Set<ScopeRepresentation> scopeSet = scopes.stream()
                .map(name -> {
                    ScopeRepresentation sr = new ScopeRepresentation();
                    sr.setName(name);
                    return sr;
                })
                .collect(Collectors.toSet());
        resource.setScopes(scopeSet);
        try(var response = keycloak.realm(realm).clients().get(clientUUID).authorization().resources().create(resource)){
            log.info("Resource created: {} Response: {}", form, response.getStatus());
        }
    }

    private void createScope(String clientUUID, String scopeName) {
        boolean exist = keycloak.realm(realm)
                .clients()
                .get(clientUUID)
                .authorization()
                .scopes()
                .findByName(scopeName) != null;

        if (exist) {
            System.out.println("Scope already exists: " + scopeName);
            return; // Skip creation
        }
        ScopeRepresentation scope = new ScopeRepresentation();
        scope.setName(scopeName);
        scope.setDisplayName(scopeName);
        keycloak.realm(realm).clients().get(clientUUID).authorization().scopes().create(scope);
    }

    public String createGroup(String groupName) {
        String url = serverUrl + "/admin/realms/" + realm + "/groups";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAdminToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = "{\"name\":\"" + groupName + "\"}";

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            System.out.println("Group created successfully: " + groupName);
        } else {
            System.err.println("Failed to create group: " + response.getStatusCode());
            System.err.println("Response body: " + response.getBody());
        }
        return groupName;
    }

    private String createGroupIfNotExists(String groupName) {
        String safeName = groupName.trim().replaceAll("\\s+", "_");

        List<GroupRepresentation> groups = Optional.ofNullable(
                keycloak.realm(realm).groups().groups()
        ).orElse(Collections.emptyList());

        return groups.stream()
                .filter(g -> g.getName().equals(safeName))
                .findFirst()
                .map(GroupRepresentation::getId)
                .orElseGet(() -> {
                    createGroup(safeName);
                   /*GroupRepresentation group = new GroupRepresentation();
                    group.setName(safeName);



                    var response = keycloak.realm(realm).groups().add(group);
                    int status = response.getStatus();

                    if (status >= 400) {
                        String errorBody = response.readEntity(String.class);
                        throw new RuntimeException("Group creation failed with status " + status + ": " + errorBody);
                    }

                    String location = response.getHeaderString("Location");
                    if (location != null && location.contains("/")) {
                        return location.substring(location.lastIndexOf('/') + 1);
                    } else {*/
                        // Fallback fetch
                        List<GroupRepresentation> updatedGroups = Optional.ofNullable(
                                keycloak.realm(realm).groups().groups()
                        ).orElse(Collections.emptyList());

                        return updatedGroups.stream()
                                .filter(g -> g.getName().equals(safeName))
                                .findFirst()
                                .map(GroupRepresentation::getId)
                                .orElseThrow(() -> new IllegalStateException("Group created but could not retrieve ID."));
                    //}
                });
    }




    private void createGroupPolicy(String clientUUID, String policyName, String groupId) {

        boolean exists = keycloak.realm(realm)
                .clients()
                .get(clientUUID)
                .authorization()
                .policies()
                .findByName(policyName) != null;
        if (exists)
            return;
        Map<String, String> config = new HashMap<>();
        String groupJson = "[{\"id\":\"" + groupId + "\",\"extendChildren\":false}]";
        config.put("groups", groupJson);


        PolicyRepresentation policy = new PolicyRepresentation();
        policy.setName(policyName);
        policy.setType("group");
        policy.setLogic(Logic.POSITIVE);
        policy.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
        policy.setConfig(config);

        keycloak.realm(realm).clients().get(clientUUID).authorization().policies().create(policy);
    }

    private void createPermission(String clientUUID, String name, String resource, String scope, String policyName) {

        boolean exists = keycloak.realm(realm)
                .clients()
                .get(clientUUID)
                .authorization()
                .permissions()
                .scope()
                .findByName(name) != null;

        if (!exists) {

            ScopePermissionRepresentation permission = new ScopePermissionRepresentation();
            permission.setName(name);
            permission.setDecisionStrategy(DecisionStrategy.UNANIMOUS);
            permission.setLogic(Logic.POSITIVE);
            permission.setResources(Set.of(resource));    // Resource names
            permission.setScopes(Set.of(scope));          // Scope names
            permission.setPolicies(Set.of(policyName));  // Already existing policy names

            keycloak.realm(realm)
                    .clients()
                    .get(clientUUID)
                    .authorization()
                    .permissions()
                    .scope()
                    .create(permission);
        }
    }

    public void assignUserToGroup(UserGroupAssignmentRequest request) {
        String userId = request.getUserId();
        String role = request.getRole();
        List<List<String>> data = request.getData();
        try {

            for (List<String> pair : data) {
                if (pair.size() != 2) continue;

                String form = pair.get(0);
                String schedule = pair.get(1);

                String groupName = role + "|" + form + "|" + schedule;

                String user = "";
                List<UserRepresentation> users = keycloak.realm(realm).users().search(userId, true);
                if (!users.isEmpty()) {
                    user =  users.get(0).getId();
                    String groupId = createGroupIfNotExists(groupName);
                    addUserToGroup(user, groupId);// Found
                } else {
                    System.out.println("User Not Found "+userId);
                }

            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void addUserToGroup(String userId, String groupId) {
        keycloak.realm(realm).users().get(userId).joinGroup(groupId);
    }



    public UserGroupAssignmentResponse getUserEntl(String userName) {
            List<UserRepresentation> users = keycloak.realm(realm).users().search(userName, true);
            if (users.isEmpty()) {
                throw new UserNotFoundException(String.format("User not found: %s", userName));
            }
            String userId = users.get(0).getId();
            UserResource userResource = keycloak.realm(realm).users().get(userId);
            List<GroupRepresentation> groups = userResource.groups();

            // Map role -> list of [form, schedule]
            Map<String, List<List<String>>> roleMap = new LinkedHashMap<>();

            for (GroupRepresentation group : groups) {
                String name = group.getName(); // e.g., ADMIN|FRY9C|Schedule_A
                String[] parts = name.split("\\|");

                if (parts.length == 3) {
                    String role = parts[0];
                    String form = parts[1];
                    String schedule = parts[2];

                    roleMap.computeIfAbsent(role, k -> new ArrayList<>())
                            .add(Arrays.asList(form, schedule));
                }
            }

            // Build list of RoleAssignments
            List<RoleAssignments> assignments = roleMap.entrySet().stream()
                    .map(e -> new RoleAssignments(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            return new UserGroupAssignmentResponse(userName, assignments);
    }


   /* public UserGroupAssignmentRequest getUserEntl(String userName) {
        try {
            List<UserRepresentation> users = keycloak.realm(realm).users().search(userName, true);
            if (users.isEmpty()) {
                throw new RuntimeException("User not found: " + username);
            }
            String userId = users.get(0).getId();

            UserResource userResource = keycloak.realm(realm).users().get(userId);
            List<GroupRepresentation> groups = userResource.groups();

            String role = null;
            List<List<String>> data = new ArrayList<>();

            for (GroupRepresentation group : groups) {
                String name = group.getName(); // e.g., Submitter_FRY9C_A
                String[] parts = name.split("\\|");

                if (parts.length == 3) {
                    String thisRole = parts[0];
                    String form = parts[1];
                    String schedule = parts[2];

                    if (role == null) {
                        role = thisRole;
                    }

                    if (role.equals(thisRole)) {
                        data.add(Arrays.asList(form, schedule));
                    }
                }
            }

            UserGroupAssignmentRequest response = new UserGroupAssignmentRequest();
            response.setUserId(userName);
            response.setRole(role);
            response.setData(data);
            return response;
        } catch (Exception e) {
            throw e;
        }

    }*/

    public void createUser(UserRequest request) {

        List<UserRepresentation> users = keycloak.realm(realm).users().search(request.getUserName(), true);
        if (users.stream().anyMatch(u -> request.getUserName().equalsIgnoreCase(u.getUsername()))) {
            throw new UserAlreadyExistsException(
                    String.format("User name already exists: %s", request.getUserName()));
        }

        // 1. Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUserName());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setEmailVerified(true);
        user.setEnabled(true);

        String userId;

        try (Response response = keycloak.realm(realm).users().create(user)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                String errorBody = response.readEntity(String.class);
                log.error("Failed to create user in Keycloak. Status: {}, Error Body: {}",
                        response.getStatus(), errorBody);

                throw new UserCreationException(
                        String.format("User creation failed. Status: %d, Error: %s",
                                response.getStatus(), errorBody));
            }

            userId = response.getLocation()
                    .getPath()
                    .replaceAll(".*/([^/]+)$", "$1");

            log.info("User [{}] created successfully in Keycloak. UserId={}",
                    request.getUserName(), userId);
        }


        // 4. Set password credential
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(request.getKey());
        cred.setTemporary(false);  // Set to true if you want the user to reset it

        keycloak.realm(realm)
                .users()
                .get(userId)
                .resetPassword(cred);

    }


    public void createRoles(List<RoleRequest> roles) {
        ClientRepresentation client = getClient(clientId);
        String clientUUID = client.getId();
        ClientResource clientResource = keycloak.realm(realm).clients().get(clientUUID);
        List<RoleRepresentation> existingRoles = getClientRoles(clientUUID);

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
                log.error("Failed to create role {}: {}", roleDTO.getName(), e.getMessage(), e);
                skipped.add(roleDTO.getName());
            }
        }

        Map<String, Object> response = Map.of(
                "created", created,
                "skipped", skipped
        );
        log.info("Role creation summary: {}", response);
    }
}

