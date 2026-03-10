package com.example.studentsystem.controller;

import com.example.studentsystem.dto.AuthRequest;
import com.example.studentsystem.dto.AuthResponse;
import com.example.studentsystem.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return new AuthResponse(authService.login(request.username(), request.password()));
    }
}
