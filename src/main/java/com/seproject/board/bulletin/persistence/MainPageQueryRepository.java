package com.seproject.board.bulletin.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.model.InternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.seproject.board.bulletin.domain.model.QMainPageMenu.mainPageMenu;
import static com.seproject.board.menu.domain.model.QMenu.menu;

@RequiredArgsConstructor
@Repository
public class MainPageQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<InternalSiteMenu> findMainPageableMenu() {
        List<Menu> fetch = jpaQueryFactory
                .select(menu).distinct()
                .from(menu)
                .where(isBoardMenu())
                .fetch();

        List<InternalSiteMenu> collect = fetch.stream()
                .map((menu) -> (InternalSiteMenu) menu)
                .collect(Collectors.toList());

        return collect;
    }

    public List<MainPageMenu> findAllWithMenu() {

        return jpaQueryFactory
                .select(mainPageMenu)
                .from(mainPageMenu)
                .leftJoin(mainPageMenu.menu,menu).fetchJoin()
                .fetch();
    }

    private BooleanExpression isBoardMenu() {
        return menu.instanceOf(BoardMenu.class);
    }

}
