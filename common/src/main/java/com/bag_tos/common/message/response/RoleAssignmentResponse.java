package com.bag_tos.common.message.response;

public class RoleAssignmentResponse {
    private String role;

    public RoleAssignmentResponse() {
    }

    public RoleAssignmentResponse(String role) {
        this.role = role;
    }

    // Getter ve Setter metodları
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}