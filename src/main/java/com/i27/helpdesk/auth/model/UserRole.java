package com.i27.helpdesk.auth.model;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
public class UserRole {

    @EmbeddedId
    private UserRoleId id;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    @JoinColumn(name = "role_id")
    private Role role;

    // ---------- Getters & Setters ----------

    public User getUser() {
        return user;
    }

    public Role getRole() {
        return role;
    }
}
