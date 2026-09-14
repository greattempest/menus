package com.tempest.menus.dto;

import java.util.Set;

public record AuthResponse(UserResponse user, Set<String> permissions) {
}