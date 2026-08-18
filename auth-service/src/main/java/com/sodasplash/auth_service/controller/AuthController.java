package com.sodasplash.auth_service.controller;

import com.sodasplash.auth_service.dto.*;
import com.sodasplash.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =====================================================
    // REGISTER
    // =====================================================

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.ok(authService.register(request));
    }

    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    // =====================================================
    // GET ALL BD USERS
    // =====================================================

    @GetMapping("/users/bd")
    public ResponseEntity<List<UserResponse>> getAllBDUsers() {
        return ResponseEntity.ok(authService.getAllBDUsers());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<UserResponse>> getAllStaff() {
        return ResponseEntity.ok(authService.getAllStaff());
    }

    // =====================================================
    // TOGGLE USER ACTIVE STATUS
    // =====================================================

    @PatchMapping("/users/{userId}/toggle-status")
    public ResponseEntity<UserResponse> toggleUserStatus(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                authService.toggleUserStatus(userId)
        );
    }

    @PatchMapping("/users/{userId}/toggle-role")
    public ResponseEntity<UserResponse> toggleUserRole(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(authService.toggleUserRole(userId));
    }
    @PostMapping("/staff")
    public ResponseEntity<AuthResponse> createStaff(
            @Valid @RequestBody CreateStaffRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.createStaff(request));
    }
}
