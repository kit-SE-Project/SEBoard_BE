package com.seproject.global.data_setup;

import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuEditAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.domain.MenuManageAuthorization;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.ExternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.board.menu.domain.repository.ExternalSiteMenuRepository;
import com.seproject.board.menu.domain.repository.MenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MenuSetup {

    @Autowired
    private MenuRepository menuRepository;
    @Autowired
    private BoardMenuRepository boardMenuRepository;
    @Autowired
    private ExternalSiteMenuRepository externalSiteMenuRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    public Menu createMenu() {
        String name = UUID.randomUUID().toString().substring(0,8);
        String description = UUID.randomUUID().toString();
        String urlInfo = UUID.randomUUID().toString().substring(0,8);
        return createMenu(name,description,urlInfo);
    }

    public BoardMenu createBoardMenu(Menu superMenu) {
        String name = UUID.randomUUID().toString().substring(0,8);
        String description = UUID.randomUUID().toString();
        String urlInfo = UUID.randomUUID().toString().substring(0,8);
        return createBoardMenu(superMenu,name,description,urlInfo);
    }

    public ExternalSiteMenu createExternalSiteMenu(Menu superMenu) {
        String name = UUID.randomUUID().toString().substring(0,8);
        String description = UUID.randomUUID().toString();
        String urlInfo = UUID.randomUUID().toString().substring(0,8);
        return createExternalSiteMenu(superMenu,name,description,urlInfo);
    }

    public Category createCategory(Menu superMenu) {
        String name = UUID.randomUUID().toString().substring(0,8);
        String description = UUID.randomUUID().toString();
        String urlInfo = UUID.randomUUID().toString().substring(0,8);
        return createCategory(superMenu,name,description,urlInfo);
    }


    public Menu createMenu(String name,String description,String urlInfo) {
        Menu menu = new Menu(null,null,name,description);
        initMenuAuthorization(menu);
        menu.changeUrlInfo(urlInfo);
        menuRepository.save(menu);
        return menu;
    }

    public BoardMenu createBoardMenu(Menu superMenu,String name,String description,String urlId) {
        BoardMenu boardMenu = new BoardMenu(null, superMenu, name, description, urlId);
        initMenuAuthorization(boardMenu);
        boardMenuRepository.save(boardMenu);
        return boardMenu;
    }

    public ExternalSiteMenu createExternalSiteMenu(Menu superMenu,String name, String description,String urlId) {
        ExternalSiteMenu externalSiteMenu = new ExternalSiteMenu(null,superMenu,name,description,urlId);
        initMenuAuthorization(externalSiteMenu);
        externalSiteMenuRepository.save(externalSiteMenu);
        return externalSiteMenu;
    }

    public Category createCategory(Menu superMenu,String name, String description,String urlId) {
        Category category = new Category(null, superMenu, name, description, urlId);
        initMenuAuthorization(category);
        categoryRepository.save(category);
        return category;
    }

    private void initMenuAuthorization(Menu menu) {
        menu.addAuthorization(new MenuAccessAuthorization(menu));
        menu.addAuthorization(new MenuExposeAuthorization(menu));
        menu.addAuthorization(new MenuEditAuthorization(menu));
        menu.addAuthorization(new MenuManageAuthorization(menu));
    }

}
