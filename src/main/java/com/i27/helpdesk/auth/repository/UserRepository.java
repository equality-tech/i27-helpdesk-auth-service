package com.i27.helpdesk.auth.repository;

import com.i27.helpdesk.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // ✅ Used by login
    Optional<User> findByEmail(String email);

    // ✅ Native query to fetch users by role
    @Query(
    value = """
        SELECT u.id, u.email, u.full_name, u.status, r.role_name
        FROM users u
        JOIN user_roles ur ON u.id = ur.user_id
        JOIN roles r ON ur.role_id = r.id
        ORDER BY u.id DESC
    """,
    nativeQuery = true
    )
    List<Object[]> findAllUsersWithRoles();


    // ✅ Assign role to user
    @Modifying
    @Transactional
    @Query(
        value = """
            INSERT INTO user_roles (user_id, role_id)
            VALUES (:userId, :roleId)
        """,
        nativeQuery = true
    )
    void assignRole(
            @Param("userId") Long userId,
            @Param("roleId") Long roleId
    );

}
