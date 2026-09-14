package com.tempest.menus.service;

import com.tempest.menus.dto.AuthResponse;
import com.tempest.menus.dto.LoginRequest;
import com.tempest.menus.dto.UserResponse;
import com.tempest.menus.entity.User;
import com.tempest.menus.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

  public static final String SESSION_USER_ID = "userId";
  public static final String SESSION_PERMISSIONS = "permissions";

  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional
  public AuthResponse login(LoginRequest request, HttpSession session) {
    if (request.getUsername() == null || request.getPassword() == null) {
      throw new IllegalArgumentException("用户名和密码不能为空");
    }

    User user = userRepository.findByUsername(request.getUsername().trim()).orElse(null);
    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("用户名或密码错误");
    }
    if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
      throw new IllegalArgumentException("用户已被停用");
    }

    Set<String> permissions = user.getRoles().stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(permission -> permission.getCode())
        .collect(Collectors.toUnmodifiableSet());
    session.setAttribute(SESSION_USER_ID, user.getId());
    session.setAttribute(SESSION_PERMISSIONS, permissions);
    return new AuthResponse(UserResponse.from(user), permissions);
  }

  public void logout(HttpSession session) {
    if (session != null) {
      session.invalidate();
    }
  }

  @Transactional(readOnly = true)
  public AuthResponse current(HttpSession session) {
    Object userId = session.getAttribute(SESSION_USER_ID);
    if (!(userId instanceof Long)) {
      return null;
    }
    User user = userRepository.findById((Long) userId).orElse(null);
    if (user == null) {
      return null;
    }
    Set<String> permissions = user.getRoles().stream()
        .flatMap(role -> role.getPermissions().stream())
        .map(permission -> permission.getCode())
        .collect(Collectors.toUnmodifiableSet());
    session.setAttribute(SESSION_PERMISSIONS, permissions);
    return new AuthResponse(UserResponse.from(user), permissions);
  }

  @SuppressWarnings("unchecked")
  public boolean hasPermission(HttpSession session, String permission) {
    Object permissions = session.getAttribute(SESSION_PERMISSIONS);
    return permissions instanceof Set<?> && ((Set<String>) permissions).contains(permission);
  }
}