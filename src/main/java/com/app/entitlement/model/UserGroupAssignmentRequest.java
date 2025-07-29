package com.app.entitlement.model;

import java.util.List;

public class UserGroupAssignmentRequest {
    private String userId;
    private String role;
    private List<List<String>> data;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<List<String>> getData() { return data; }
    public void setData(List<List<String>> data) { this.data = data; }
}

