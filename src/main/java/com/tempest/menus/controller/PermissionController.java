package com.tempest.menus.controller;

import com.tempest.menus.entity.Permission;
import com.tempest.menus.repository.PermissionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

  private final PermissionRepository permissionRepository;

  public PermissionController(PermissionRepository permissionRepository) {
    this.permissionRepository = permissionRepository;
  }

  @GetMapping
  public List<Permission> findAll() {
    return permissionRepository.findAll();
  }

  @PostMapping
  public ResponseEntity<Permission> create(@RequestBody Permission permission) {
    return ResponseEntity.status(HttpStatus.CREATED).body(permissionRepository.save(permission));
  }
}