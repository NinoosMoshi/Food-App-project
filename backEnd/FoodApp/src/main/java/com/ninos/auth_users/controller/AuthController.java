package com.ninos.auth_users.controller;

import com.ninos.auth_users.dtos.LoginRequest;
import com.ninos.auth_users.dtos.LoginResponse;
import com.ninos.auth_users.dtos.RegistrationRequest;
import com.ninos.auth_users.services.AuthService;
import com.ninos.response.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Response<?>> register(@RequestBody @Valid RegistrationRequest registrationRequest) {
        return ResponseEntity.ok(authService.register(registrationRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<Response<LoginResponse>> login(@RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }


}
