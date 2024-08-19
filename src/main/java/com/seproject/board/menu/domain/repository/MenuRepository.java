package com.seproject.board.menu.domain.repository;

import com.seproject.board.menu.domain.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    boolean existsByUrlInfo(String urlInfo);
    @Query("select m from Menu m where m.depth = :depth order by m.menuId ASC ")
    List<Menu> findByDepth(@Param("depth") int depth);
    @Query("select m from Menu m join fetch m.superMenu sm where sm.menuId = :superMenuId order by m.menuId ASC ")
    List<Menu> findBySuperMenu(@Param("superMenuId") Long superMenuId);

    @Query("select m from Menu m join fetch m.superMenu sm where sm.menuId in :superMenuIds order by m.menuId ASC ")
    List<Menu> findBySuperMenu(@Param("superMenuIds") List<Long> superMenuIds);
    @Query("select case when count(m) > 0 then true else false end from Menu m where m.superMenu.menuId = :menuId")
    boolean existsSubMenuById(@Param("menuId") Long menuId);


    @Query("select menu from Menu menu where menu.superMenu.menuId in :superMenuIds order by menu.menuId ASC ")
    List<Menu> findAllBySuperMenuIds(@Param("superMenuIds") List<Long> superMenuIds);

    @Query("select distinct m from Menu m left outer join fetch m.menuAuthorizations auth where m.superMenu.menuId = :superMenuId order by m.menuId ASC")
    List<Menu> findBySuperMenuWithAuthorization(@Param("superMenuId") Long superMenuId);

    @Query("select m from Menu m join fetch m.superMenu sm where m.depth = :depth order by m.menuId ASC")
    List<Menu> findByDepthWithSuperMenu(@Param("depth") int depth);
}
