package com.seproject.admin.dashboard.domain.repository;

import com.seproject.admin.dashboard.domain.DashBoardMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashBoardMenuRepository extends JpaRepository<DashBoardMenu, Long> {

    @Query("select distinct m from DashBoardMenu m join fetch m.dashBoardMenuAuthorizations auth join fetch auth.role role where m.id in :ids")
    List<DashBoardMenu> findDashBoardMenusWithRole(@Param("ids") List<Long> ids);

    boolean existsByName(String name);

    @Query("select m from DashBoardMenu m join fetch m.dashBoardMenuAuthorizations auth join fetch auth.role role where m.url=:url")
    Optional<DashBoardMenu> findByUrl(String url);
}
