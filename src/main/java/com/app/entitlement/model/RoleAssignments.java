package com.app.entitlement.model;

import lombok.Data;

import java.util.List;

@Data
public class RoleAssignments {

    private String role;
    private List<List<String>> data;

    public RoleAssignments(String role, List<List<String>> data) {
        this.role = role;
        this.data = data;
    }
}
