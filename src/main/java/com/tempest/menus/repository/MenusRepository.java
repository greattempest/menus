package com.tempest.menus.repository;

import com.tempest.menus.entity.Menus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface MenusRepository extends JpaRepository<Menus, Integer> {

  List<Menus> findByDeleteFlag(Integer deleteFlag);

  @Query("select m from Menus m where m.deleteFlag = 0 and "
      + "(lower(m.name) like lower(concat('%', :keyword, '%')) "
      + "or lower(m.material) like lower(concat('%', :keyword, '%')) "
      + "or lower(m.tag) like lower(concat('%', :keyword, '%')))")
  List<Menus> findActiveByKeyword(@Param("keyword") String keyword);
}
