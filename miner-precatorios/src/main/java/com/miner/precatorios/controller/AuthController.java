package com.miner.precatorios.controller;

import com.miner.precatorios.dto.LoginRequest;
import com.miner.precatorios.dto.LoginResponse;
import com.miner.precatorios.dto.RegisterRequest;
import com.miner.precatorios.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth") // CORREÇÃO: Adicionado /api para bater com o SecurityConfig
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        // Agora acessível via POST http://localhost:8080/api/auth/login
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok().build();
    }
}