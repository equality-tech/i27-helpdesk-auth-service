package com.i27.helpdesk.auth.controller;

import com.i27.helpdesk.auth.dto.CreateUserRequest;
import com.i27.helpdesk.auth.dto.UserWithRoleDTO;
import com.i27.helpdesk.auth.model.User;
import com.i27.helpdesk.auth.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/admin")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // =====================================================
    // 🔐 ADMIN: Create User (STUDENT / AGENT / ADMIN)
    // =====================================================
    @PostMapping("/users")
    public ResponseEntity<?> createUser(
            @RequestBody CreateUserRequest request
    ) {
        User user = adminUserService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new UserResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFullName(),
                        user.getStatus()
                ));
    }

    // =====================================================
    // 🔐 ADMIN: List ALL Users WITH ROLES ✅
    // =====================================================
    @GetMapping("/users")
    public ResponseEntity<List<UserWithRoleDTO>> getAllUsers() {
        return ResponseEntity.ok(
                adminUserService.getAllUsers()
        );
    }

    // =====================================================
    // ❌ DUPLICATE USER HANDLER
    // =====================================================
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleDuplicateUser(
            IllegalStateException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
    }

    // =====================================================
    // ❌ INVALID ROLE HANDLER
    // =====================================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleInvalidRole(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    // =====================================================
    // ✅ SAFE RESPONSE DTO (CREATE USER)
    // =====================================================
    record UserResponse(
            Long id,
            String email,
            String fullName,
            String status
    ) {}
}
