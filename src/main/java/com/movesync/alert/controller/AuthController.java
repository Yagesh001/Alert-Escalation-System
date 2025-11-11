package com.movesync.alert.controller;

import com.movesync.alert.dto.ApiResponse;
import com.movesync.alert.dto.AuthResponse;
import com.movesync.alert.dto.LoginRequest;
import com.movesync.alert.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Authentication
 * Handles user login and token generation
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication")
public class AuthController {

    private final AuthService authService;

    /**
     * User login endpoint
     * POST /api/v1/auth/login
     * 
     * @param request Login credentials
     * @return JWT token and user info
     */
    @PostMapping("/login")
    @Operation(summary = "User login", 
               description = "Authenticate user and generate JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        
        log.info("Login attempt for user: {}", request.getUsername());
        
        AuthResponse response = authService.authenticate(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Health check endpoint (no auth required)
     * GET /api/v1/auth/health
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if auth service is running")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is running"));
    }
}

