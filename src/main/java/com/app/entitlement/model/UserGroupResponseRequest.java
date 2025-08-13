package com.app.entitlement.model;

import lombok.Data;

import java.util.List;

@Data
public class UserGroupResponseRequest {
    private String userId;
    private List<RoleAssignments> assignments;

    public UserGroupResponseRequest(String userId, List<RoleAssignments> assignments) {
        this.userId = userId;
        this.assignments = assignments;
    }


}

