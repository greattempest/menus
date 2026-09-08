package com.tempest.menus.service;

import com.tempest.menus.entity.Menus;
import com.tempest.menus.repository.MenusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MenusService {

  private final MenusRepository menusRepository;

  @Autowired
  public MenusService(MenusRepository menusRepository) {
    this.menusRepository = menusRepository;
  }

  public List<Menus> findAll() {
    return menusRepository.findByDeleteFlag(0);
  }

  public List<Menus> findByKeyword(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return menusRepository.findByDeleteFlag(0);
    }

    String normalizedKeyword = keyword.trim();
    return menusRepository.findActiveByKeyword(normalizedKeyword);
  }

  public Optional<Menus> findById(Integer id) {
    return menusRepository.findById(id);
  }

  public Menus save(Menus menus) {
    return menusRepository.save(menus);
  }

  public boolean deleteById(Integer id) {
    return menusRepository.findById(id)
        .map(menu -> {
          menu.setDeleteFlag(1);
          menusRepository.save(menu);
          return true;
        })
        .orElse(false);
  }
}
