package com.i27.helpdesk.auth.service;

import java.util.List;

public class LoginResponse {

    private String token;
    private Long userId;
    private String email;
    private String fullName;
    private List<String> roles;

    public LoginResponse(String token, Long userId, String email, String fullName, List<String> roles) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.roles = roles;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public List<String> getRoles() {
        return roles;
    }
}
