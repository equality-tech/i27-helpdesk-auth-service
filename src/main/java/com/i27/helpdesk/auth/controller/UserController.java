package com.i27.helpdesk.auth.controller;

import com.i27.helpdesk.auth.dto.UserWithRoleDTO;
import com.i27.helpdesk.auth.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // =====================================================
    // 🔐 Get user by ID (used by ticket enrichment)
    // =====================================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {

        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(
                        new UserDTO(
                                user.getId(),
                                user.getEmail(),
                                user.getFullName()
                        )
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    // =====================================================
    // 🔐 Get users (optionally filtered by role)
    // Used by Admin Dashboard & Assign Agent dropdown
    // =====================================================
    @GetMapping
    public ResponseEntity<List<UserWithRoleDTO>> getUsers(
            @RequestParam(required = false) String role
    ) {

        List<UserWithRoleDTO> users =
                userRepository.findAllUsersWithRoles()
                        .stream()
                        .filter(row ->
                                role == null ||
                                row[4].toString().equalsIgnoreCase(role)
                        )
                        .map(row -> new UserWithRoleDTO(
                                ((Number) row[0]).longValue(), // id
                                (String) row[1],               // email
                                (String) row[2],               // fullName
                                (String) row[3],               // status
                                (String) row[4]                // role
                        ))
                        .toList();

        return ResponseEntity.ok(users);
    }

    // =====================================================
    // ✅ SAFE DTO (NO PASSWORD)
    // =====================================================
    public record UserDTO(
            Long id,
            String email,
            String fullName
    ) {}
}
