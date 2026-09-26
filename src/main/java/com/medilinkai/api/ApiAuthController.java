package com.medilinkai.api;

import com.medilinkai.model.User;
import com.medilinkai.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST auth for the static portal pages (landing.html sign in / sign up).
 * Wire format expected by the portal's app.js:
 *   login  -> {status:'SUCCESS', id, name, email, role, ...}
 *           | {status:'ERROR', code:'USER_NOT_FOUND'|..., message}
 *   signup -> {status:'SUCCESS'} | {status:'ERROR', message}
 */
@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final UserService userService;

    public ApiAuthController(UserService userService) {
        this.userService = userService;
    }

    public record LoginRequest(String email, String password) {
    }

    public record RegisterRequest(String name, String email, String password,
                                  String role, String extra) {
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest req, HttpSession session) {
        String email = req.email() == null ? "" : req.email().trim();
        var found = userService.findByEmail(email);
        if (found.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "code", "USER_NOT_FOUND",
                    "message", "No account found for " + email));
        }
        User user = found.get();
        if (!user.getPassword().equals(req.password() == null ? "" : req.password())) {
            return ResponseEntity.ok(Map.of(
                    "status", "ERROR",
                    "code", "WRONG_PASSWORD",
                    "message", "Wrong password. Please try again."));
        }
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "id", String.valueOf(user.getId()),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "phone", user.getPhone() == null ? "" : user.getPhone()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req, HttpSession session) {
        String email = req.email() == null ? "" : req.email().trim();
        if (email.isBlank() || req.password() == null || req.password().isBlank()) {
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", "Email and password are required."));
        }
        if (userService.findByEmail(email).isPresent()) {
            return ResponseEntity.ok(Map.of("status", "ERROR", "message", "This email is already registered."));
        }
        User user = userService.registerWithRole(req.name(), email, req.password(), req.role(), req.extra());
        session.setAttribute("userId", user.getId());
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "id", String.valueOf(user.getId()),
                "name", user.getName(),
                "role", user.getRole().name()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("status", "SUCCESS"));
    }
}
