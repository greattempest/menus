package com.tempest.menus.controller;

import com.tempest.menus.dto.UserRequest;
import com.tempest.menus.dto.UserResponse;
import com.tempest.menus.entity.User;
import com.tempest.menus.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public List<UserResponse> findAll() {
    return userService.findAll().stream().map(UserResponse::from).toList();
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
    User user = userService.findById(id);
    return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(UserResponse.from(user));
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody UserRequest request) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(userService.create(request)));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.badRequest().body(exception.getMessage());
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UserRequest request) {
    try {
      User user = userService.update(id, request);
      return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(UserResponse.from(user));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.badRequest().body(exception.getMessage());
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    return userService.delete(id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
  }
}