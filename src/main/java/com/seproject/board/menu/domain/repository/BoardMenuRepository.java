package com.seproject.board.menu.domain.repository;

import com.seproject.board.menu.domain.model.BoardMenu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardMenuRepository extends JpaRepository<BoardMenu, Long> {
    boolean existsByName(String name);

    boolean existsByUrlInfo(String urlInfo);
}
