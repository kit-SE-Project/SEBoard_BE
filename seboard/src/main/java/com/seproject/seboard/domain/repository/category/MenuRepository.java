package com.seproject.seboard.domain.repository.category;

import com.seproject.seboard.controller.dto.post.CategoryResponse;
import com.seproject.seboard.domain.model.category.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    List<Menu> findByDepth(int depth);
    List<Menu> findBySuperMenu(Long superMenuId);
    @Query("select case when count(m) > 0 then true else false end from Menu m where m.superMenu.menuId = :menuId")
    boolean existsSubMenuById(Long menuId);
}