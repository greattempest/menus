package com.tempest.menus.service;

import com.tempest.menus.dto.UserRequest;
import com.tempest.menus.entity.Role;
import com.tempest.menus.entity.User;
import com.tempest.menus.repository.RoleRepository;
import com.tempest.menus.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public UserService(UserRepository userRepository, RoleRepository roleRepository) {
    this.userRepository = userRepository;
    this.roleRepository = roleRepository;
  }

  @Transactional(readOnly = true)
  public List<User> findAll() {
    return userRepository.findAll();
  }

  @Transactional(readOnly = true)
  public User findById(Long id) {
    return userRepository.findById(id).orElse(null);
  }

  @Transactional
  public User create(UserRequest request) {
    String username = required(request.getUsername(), "用户名不能为空");
    String password = required(request.getPassword(), "密码不能为空");
    if (userRepository.existsByUsername(username)) {
      throw new IllegalArgumentException("用户名已存在");
    }

    User user = new User();
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(password));
    apply(user, request);
    user.markCreated();
    return userRepository.save(user);
  }

  @Transactional
  public User update(Long id, UserRequest request) {
    User user = userRepository.findById(id).orElse(null);
    if (user == null) {
      return null;
    }
    if (request.getUsername() != null && !request.getUsername().isBlank()
        && !request.getUsername().equals(user.getUsername())
        && userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException("用户名已存在");
    }
    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      user.setUsername(request.getUsername().trim());
    }
    if (request.getPassword() != null && !request.getPassword().isBlank()) {
      user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    }
    apply(user, request);
    user.markUpdated();
    return userRepository.save(user);
  }

  @Transactional
  public boolean delete(Long id) {
    if (!userRepository.existsById(id)) {
      return false;
    }
    userRepository.deleteById(id);
    return true;
  }

  private void apply(User user, UserRequest request) {
    user.setNickname(request.getNickname());
    user.setEmail(request.getEmail());
    user.setPhone(request.getPhone());
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      user.setStatus(request.getStatus().trim().toUpperCase());
    }
    if (request.getRoleIds() != null) {
      Set<Role> roles = new LinkedHashSet<>(roleRepository.findByIdIn(request.getRoleIds()));
      if (roles.size() != request.getRoleIds().size()) {
        throw new IllegalArgumentException("包含不存在的角色");
      }
      user.setRoles(roles);
    }
  }

  private String required(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }
    return value.trim();
  }
}