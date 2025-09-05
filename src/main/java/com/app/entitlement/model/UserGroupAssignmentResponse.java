package com.app.entitlement.model;

import lombok.Data;

import java.util.List;

@Data
public class UserGroupAssignmentResponse {
    private String userId;
    private List<RoleAssignments> assignments;

    public UserGroupAssignmentResponse(String userId, List<RoleAssignments> assignments) {
        this.userId = userId;
        this.assignments = assignments;
    }


}

