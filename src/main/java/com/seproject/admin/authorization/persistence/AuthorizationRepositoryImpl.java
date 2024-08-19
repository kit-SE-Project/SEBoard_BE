package com.seproject.admin.authorization.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.account.authorization.domain.*;
import com.seproject.account.authorization.domain.repository.AuthorizationRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;


import java.util.Optional;

import static com.seproject.account.authorization.domain.QMenuAccessAuthorization.menuAccessAuthorization;
import static com.seproject.account.authorization.domain.QMenuEditAuthorization.menuEditAuthorization;
import static com.seproject.account.authorization.domain.QMenuExposeAuthorization.menuExposeAuthorization;
import static com.seproject.account.authorization.domain.QMenuManageAuthorization.menuManageAuthorization;
import static com.seproject.board.menu.domain.model.QMenu.menu;

@RequiredArgsConstructor
@Repository
public class AuthorizationRepositoryImpl implements AuthorizationRepositoryCustom {

    private final JPAQueryFactory jpaQueryFactory;
    @Override
    public Optional<MenuAccessAuthorization> findMenuAccessAuthorization(Long menuId) {
        MenuAccessAuthorization one = jpaQueryFactory
                .select(menuAccessAuthorization)
                .from(menuAccessAuthorization)
                .leftJoin(menuAccessAuthorization.menu, menu).fetchJoin()
                .where(menu.menuId.eq(menuId))
                .fetchOne();
        return Optional.ofNullable(one);
    }

    @Override
    public Optional<MenuManageAuthorization> findMenuManageAuthorization(Long menuId) {
        MenuManageAuthorization one = jpaQueryFactory
                .select(menuManageAuthorization)
                .from(menuManageAuthorization)
                .leftJoin(menuManageAuthorization.menu, menu).fetchJoin()
                .where(menu.menuId.eq(menuId))
                .fetchOne();
        return Optional.ofNullable(one);
    }

    @Override
    public Optional<MenuEditAuthorization> findMenuEditAuthorization(Long menuId) {
        MenuEditAuthorization one = jpaQueryFactory
                .select(menuEditAuthorization)
                .from(menuEditAuthorization)
                .leftJoin(menuEditAuthorization.menu,menu).fetchJoin()
                .where(menu.menuId.eq(menuId))
                .fetchOne();
        return Optional.ofNullable(one);
    }

    @Override
    public Optional<MenuExposeAuthorization> findMenuExposeAuthorization(Long menuId) {
        MenuExposeAuthorization one = jpaQueryFactory
                .select(menuExposeAuthorization)
                .from(menuExposeAuthorization)
                .leftJoin(menuExposeAuthorization.menu,menu).fetchJoin()
                .where(menu.menuId.eq(menuId))
                .fetchOne();
        return Optional.ofNullable(one);
    }

        //TODO : 메뉴가 자기 자식도 갖는 양방향 구조였으면 좋겠다.










}
