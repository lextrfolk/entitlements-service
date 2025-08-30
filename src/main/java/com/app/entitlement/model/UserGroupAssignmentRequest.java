package com.app.entitlement.model;

import lombok.Data;
import java.util.List;

/**
 * DTO representing a request to assign a user to one or more groups with a role.
 */
@Data
public class UserGroupAssignmentRequest {
    private String userId;
    private String role;
    private List<List<String>> data;

}

