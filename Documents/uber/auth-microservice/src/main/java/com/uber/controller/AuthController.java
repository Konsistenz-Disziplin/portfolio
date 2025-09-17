// controller/AuthController.java
package com.uber.controller;

import com.uber.dto.RegisterRequest;
import com.uber.dto.LoginRequest;
import com.uber.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService keycloakUserService;

    public AuthController(UserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        keycloakUserService.registerUser(request.getUsername(), request.getEmail(), request.getPassword(), request.getRole());
        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String tokenResponse = keycloakUserService.login(request.getUsername(), request.getPassword());
        return ResponseEntity.ok(tokenResponse);
    }
}
