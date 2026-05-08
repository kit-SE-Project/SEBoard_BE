package com.seproject.board.menu.domain.repository;

import com.seproject.board.menu.domain.model.RecruitMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RecruitMenuRepository extends JpaRepository<RecruitMenu, Long> {

    @Query("SELECT r FROM RecruitMenu r LEFT JOIN FETCH r.menuAuthorizations WHERE r.urlInfo = 'recruit'")
    Optional<RecruitMenu> findWithAuthorizations();
}
