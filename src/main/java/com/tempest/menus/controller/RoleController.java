package com.tempest.menus.controller;

import com.tempest.menus.dto.RoleRequest;
import com.tempest.menus.entity.Role;
import com.tempest.menus.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

  private final RoleService roleService;

  public RoleController(RoleService roleService) {
    this.roleService = roleService;
  }

  @GetMapping
  public List<Role> findAll() {
    return roleService.findAll();
  }

  @PostMapping
  public ResponseEntity<?> create(@RequestBody RoleRequest request) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    } catch (IllegalArgumentException exception) {
      return ResponseEntity.badRequest().body(exception.getMessage());
    }
  }
}