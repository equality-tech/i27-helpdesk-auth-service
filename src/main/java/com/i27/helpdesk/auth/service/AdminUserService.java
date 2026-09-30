package com.i27.helpdesk.auth.service;

import com.i27.helpdesk.auth.dto.CreateUserRequest;
import com.i27.helpdesk.auth.model.Role;
import com.i27.helpdesk.auth.model.User;
import com.i27.helpdesk.auth.repository.RoleRepository;
import com.i27.helpdesk.auth.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.i27.helpdesk.auth.dto.UserWithRoleDTO;
import java.util.List;


@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AdminUserService(
            UserRepository userRepository,
            RoleRepository roleRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public User createUser(CreateUserRequest request) {

        // ✅ Duplicate email check
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException(
                    "User already exists with email: " + request.getEmail()
            );
        }

        // ✅ Role validation
        Role role = roleRepository
                .findByRoleName(request.getRole())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid role: " + request.getRole())
                );

        // ✅ Create user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setStatus("ACTIVE");

        User savedUser = userRepository.save(user);

        // 🔥 FIX IS HERE — Integer → Long conversion
        Long roleId = Long.valueOf(role.getId());

        userRepository.assignRole(
                savedUser.getId(), // Long
                roleId              // Long
        );

        return savedUser;
    }

    // List users with their roles
    public List<UserWithRoleDTO> getAllUsers() {
        return userRepository.findAllUsersWithRoles()
            .stream()
            .map(row -> new UserWithRoleDTO(
                ((Number) row[0]).longValue(), // id
                (String) row[1],               // email
                (String) row[2],               // full_name
                (String) row[3],               // status
                (String) row[4]                // role_name

                ))
            .toList();
    }
}
