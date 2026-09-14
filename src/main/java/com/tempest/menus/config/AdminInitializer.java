package com.tempest.menus.config;

import com.tempest.menus.entity.Permission;
import com.tempest.menus.entity.Role;
import com.tempest.menus.entity.User;
import com.tempest.menus.repository.PermissionRepository;
import com.tempest.menus.repository.RoleRepository;
import com.tempest.menus.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Component
public class AdminInitializer implements CommandLineRunner {

  private static final List<String> PERMISSIONS = List.of(
      "menu:create", "menu:update", "menu:delete", "user:manage", "role:manage", "permission:manage");

  private final PermissionRepository permissionRepository;
  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AdminInitializer(PermissionRepository permissionRepository, RoleRepository roleRepository,
      UserRepository userRepository) {
    this.permissionRepository = permissionRepository;
    this.roleRepository = roleRepository;
    this.userRepository = userRepository;
  }

  @Override
  @Transactional
  public void run(String... args) {
    List<Permission> permissions = PERMISSIONS.stream()
        .map(code -> permissionRepository.findByCode(code).orElseGet(() -> createPermission(code)))
        .toList();
    Role adminRole = roleRepository.findByCode("ADMIN").orElseGet(() -> {
      Role role = new Role();
      role.setCode("ADMIN");
      role.setName("系统管理员");
      return role;
    });
    adminRole.setPermissions(new LinkedHashSet<>(permissions));
    adminRole = roleRepository.save(adminRole);

    User admin = userRepository.findByUsername("admin").orElseGet(User::new);
    boolean newAdmin = admin.getId() == null;
    admin.setUsername("admin");
    if (newAdmin) {
      admin.setPasswordHash(passwordEncoder.encode("123456,a"));
    }
    admin.setNickname("系统管理员");
    admin.setStatus("ACTIVE");
    admin.setRoles(new LinkedHashSet<>(List.of(adminRole)));
    if (admin.getCreatedAt() == null) {
      admin.markCreated();
    } else {
      admin.markUpdated();
    }
    userRepository.save(admin);
  }

  private Permission createPermission(String code) {
    Permission permission = new Permission();
    permission.setCode(code);
    permission.setName(code);
    return permissionRepository.save(permission);
  }
}