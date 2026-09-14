package com.tempest.menus.service;

import com.tempest.menus.dto.RoleRequest;
import com.tempest.menus.entity.Permission;
import com.tempest.menus.entity.Role;
import com.tempest.menus.repository.PermissionRepository;
import com.tempest.menus.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
public class RoleService {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;

  public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository) {
    this.roleRepository = roleRepository;
    this.permissionRepository = permissionRepository;
  }

  @Transactional(readOnly = true)
  public List<Role> findAll() {
    return roleRepository.findAll();
  }

  @Transactional
  public Role create(RoleRequest request) {
    if (request.getCode() == null || request.getCode().isBlank()
        || request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("角色编码和名称不能为空");
    }
    Role role = new Role();
    role.setCode(request.getCode().trim());
    role.setName(request.getName().trim());
    role.setDescription(request.getDescription());
    if (request.getPermissionIds() != null) {
      List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
      if (permissions.size() != request.getPermissionIds().size()) {
        throw new IllegalArgumentException("包含不存在的权限");
      }
      role.setPermissions(new LinkedHashSet<>(permissions));
    }
    return roleRepository.save(role);
  }
}