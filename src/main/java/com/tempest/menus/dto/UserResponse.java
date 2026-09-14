package com.tempest.menus.dto;

import com.tempest.menus.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(Long id, String username, String nickname, String email, String phone,
    String status, LocalDateTime createdAt, LocalDateTime updatedAt, List<String> roles) {

  public static UserResponse from(User user) {
    return new UserResponse(
        user.getId(),
        user.getUsername(),
        user.getNickname(),
        user.getEmail(),
        user.getPhone(),
        user.getStatus(),
        user.getCreatedAt(),
        user.getUpdatedAt(),
        user.getRoles().stream().map(role -> role.getCode()).sorted().toList());
  }
}