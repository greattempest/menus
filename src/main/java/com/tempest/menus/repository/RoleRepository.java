package com.tempest.menus.repository;

import com.tempest.menus.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

  List<Role> findByIdIn(Collection<Long> ids);

  Optional<Role> findByCode(String code);
}