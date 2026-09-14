package com.tempest.menus.dto;

import java.util.Set;

public class RoleRequest {

  private String code;
  private String name;
  private String description;
  private Set<Long> permissionIds;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Set<Long> getPermissionIds() {
    return permissionIds;
  }

  public void setPermissionIds(Set<Long> permissionIds) {
    this.permissionIds = permissionIds;
  }
}