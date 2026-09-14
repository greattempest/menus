package com.tempest.menus.controller;

import com.tempest.menus.dto.AuthResponse;
import com.tempest.menus.dto.LoginRequest;
import com.tempest.menus.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpSession session) {
    try {
      return ResponseEntity.ok(authService.login(request, session));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.status(401).body(exception.getMessage());
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpSession session) {
    authService.logout(session);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<AuthResponse> current(HttpSession session) {
    AuthResponse response = authService.current(session);
    return response == null ? ResponseEntity.status(401).build() : ResponseEntity.ok(response);
  }
}