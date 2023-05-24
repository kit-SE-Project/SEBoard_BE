package com.seproject.admin.domain.repository;

import com.seproject.admin.domain.MainPageMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MainPageMenuRepository extends JpaRepository<MainPageMenu,Long> {
    @Query("select menu from MainPageMenu menu join fetch menu.menu")
    List<MainPageMenu> findAll();
}
