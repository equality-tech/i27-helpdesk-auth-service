package com.i27.helpdesk.auth.controller;

import com.i27.helpdesk.auth.service.AuthService;
import com.i27.helpdesk.auth.service.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ==============================
    // 🔐 LOGIN
    // ==============================
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody Map<String, String> request) {

        LoginResponse response = authService.login(
                request.get("email"),
                request.get("password")
        );

        return ResponseEntity.ok(response);
    }

    // ==============================
    // 🔐 ADMIN → RESET USER PASSWORD
    // ==============================
    @PutMapping("/users/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {

        String rolesHeader = httpRequest.getHeader("X-User-Roles");
        if (rolesHeader == null || !rolesHeader.contains("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String password = request.get("password");
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body("Password must be at least 6 characters");
        }

        authService.resetUserPassword(id, password);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok(
                Map.of("status", "AUTH SERVICE PONG")
        );
    }
}
