package com.app.entitlement.service;

import com.app.entitlement.model.UserGroupAssignmentRequest;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.*;
import org.keycloak.representations.idm.authorization.*;
import org.springframework.beans.factory.annotation.Value;
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
public class KeycloakAuthorizationProvisionerService {

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.username}")
    private String username;

    @Value("${keycloak.password}")
    private String password;

    private Keycloak keycloak;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostConstruct
    public void init() {
        keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .clientId(clientId)
                .clientSecret("hobI81AZuI4w2PLPSMq1xAbAzgEYOvII")
                .username(username)
                .password(password)
                .grantType(OAuth2Constants.PASSWORD)
                .build();
    }

    private String getAdminToken() {
        String url = serverUrl + "/realms/entlrealm/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> params = Map.of(
                "client_id", clientId,
                "client_secret","hobI81AZuI4w2PLPSMq1xAbAzgEYOvII",
                "grant_type", "password",
                "username", username,
                "password", password
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
        return keycloak.realm(realm).clients().findByClientId(clientId).get(0);
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
        keycloak.realm(realm).clients().get(clientUUID).authorization().resources().create(resource);
    }

    private void createScope(String clientUUID, String scopeName) {
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
                String user = getOrCreateUserByUsername(userId);
                String groupId = createGroupIfNotExists(groupName);
                addUserToGroup(user, groupId);
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    private void addUserToGroup(String userId, String groupId) {
        keycloak.realm(realm).users().get(userId).joinGroup(groupId);
    }

    private String createUser(String username, String firstName, String lastName, String password) {
        // 1. Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(username+"@example.com");
        user.setEmailVerified(true);
        user.setEnabled(true);

        // 2. Send create request
        Response response = keycloak.realm(realm).users().create(user);

        if (response.getStatus() != 201) {
            System.out.println("Error Body: " + response.readEntity(String.class));
            return "Failed to create user. Status: " + response.getStatus();
        }

        // 3. Extract created userId
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");
        response.close();

        // 4. Set password credential
        CredentialRepresentation cred = new CredentialRepresentation();
        cred.setType(CredentialRepresentation.PASSWORD);
        cred.setValue(password);
        cred.setTemporary(false);  // Set to true if you want the user to reset it

        keycloak.realm(realm)
                .users()
                .get(userId)
                .resetPassword(cred);

        return userId;
    }

    private String getOrCreateUserByUsername(String username) {
        List<UserRepresentation> users = keycloak.realm(realm).users().search(username, true);
        if (!users.isEmpty()) {
            return users.get(0).getId(); // Found
        }

        // Create user if not exists
       return createUser(username, username+"firstname", username+"lastname","test123");
    }

    public UserGroupAssignmentRequest getUserEntl(String userName) {
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

    }
}

