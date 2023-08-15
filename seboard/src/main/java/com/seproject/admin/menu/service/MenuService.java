package com.seproject.admin.menu.service;

import com.seproject.account.authorization.domain.MenuAccessAuthorization;
import com.seproject.account.authorization.domain.MenuEditAuthorization;
import com.seproject.account.authorization.domain.MenuExposeAuthorization;
import com.seproject.account.authorization.domain.MenuManageAuthorization;
import com.seproject.board.menu.domain.BoardMenu;
import com.seproject.board.menu.domain.Category;
import com.seproject.board.menu.domain.ExternalSiteMenu;
import com.seproject.board.menu.domain.Menu;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.board.menu.domain.repository.ExternalSiteMenuRepository;
import com.seproject.board.menu.domain.repository.MenuRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.NoSuchResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.seproject.board.common.utils.AppServiceHelper.findByIdOrThrow;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class MenuService {

    private final MenuRepository menuRepository;
    private final BoardMenuRepository boardMenuRepository;
    private final CategoryRepository categoryRepository;
    private final ExternalSiteMenuRepository externalSiteMenuRepository;

    public List<Menu> findByIds(List<Long> ids) {
        return menuRepository.findAllById(ids);
    }
    public Menu findById(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU, null));
    }

    public Map<Menu, List<Menu>> findAllMenuTree() {
        List<Menu> groupMenus = menuRepository.findByDepthWithSuperMenu(1);
        return groupMenus.stream()
                .collect(Collectors.groupingBy(Menu::getSuperMenu));
    }

    public List<Menu> findSubMenu(Long superMenuId) {
        return menuRepository.findBySuperMenu(superMenuId);
    }

    private void addMenuAuthorization(Menu menu) {
        MenuExposeAuthorization expose = new MenuExposeAuthorization(menu);
        MenuManageAuthorization manage = new MenuManageAuthorization(menu);
        MenuEditAuthorization edit = new MenuEditAuthorization(menu);
        MenuAccessAuthorization access = new MenuAccessAuthorization(menu);
        menu.addAuthorization(expose);
        menu.addAuthorization(manage);
        menu.addAuthorization(edit);
        menu.addAuthorization(access);
    }

    @Transactional
    public Long createMenu(Menu superMenu, String name, String description,String urlInfo) {
        if(!StringUtils.hasText(urlInfo)) {
            urlInfo = UUID.randomUUID().toString().substring(0,8);
        }

        Menu menu = new Menu(null,superMenu,name,description);
        menu.changeUrlInfo(urlInfo);

        addMenuAuthorization(menu);
        menuRepository.save(menu);

        return menu.getMenuId();
    }

    @Transactional
    public Long createBoardMenu(Menu superMenu,String name, String description,String urlInfo) {

        if(!StringUtils.hasText(urlInfo)) {
            urlInfo = UUID.randomUUID().toString().substring(0,8);
        }

        BoardMenu boardMenu = new BoardMenu(null,superMenu,name,description,urlInfo);
        addMenuAuthorization(boardMenu);
        boardMenuRepository.save(boardMenu);

        Category category = new Category(null, boardMenu, "일반", "기본으로 생성되는 게시판 카테고리", UUID.randomUUID().toString().substring(0, 8));
        addMenuAuthorization(category);
        categoryRepository.save(category);

        return boardMenu.getMenuId();
    }

    @Transactional
    public Long createCategory(Menu superMenu,String name, String description,String urlInfo) {
        if(!StringUtils.hasText(urlInfo)) {
            urlInfo = UUID.randomUUID().toString().substring(0,8);
        }

        Category category = new Category(null,superMenu,name,description,urlInfo);
        addMenuAuthorization(category);
        categoryRepository.save(category);

        return category.getMenuId();
    }

    @Transactional
    public Long createExternalSiteMenu(Menu superMenu,String name, String description, String urlInfo) {
        ExternalSiteMenu externalSiteMenu = new ExternalSiteMenu(null, superMenu, name, description, urlInfo);
        addMenuAuthorization(externalSiteMenu);
        externalSiteMenuRepository.save(externalSiteMenu);

        return externalSiteMenu.getMenuId();
    }

    public void delete(Long menuId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_MENU,null));

        if(menuRepository.existsSubMenuById(menuId)){
            throw new CustomIllegalArgumentException(ErrorCode.CANNOT_DELETE_MENU,null);
        }

        menuRepository.delete(menu);
    }

    public void changeSuperCategory(Long fromMenuId, Long toMenuId) {
        BoardMenu from = boardMenuRepository.findById(fromMenuId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MENU));
        BoardMenu to = boardMenuRepository.findById(toMenuId)
                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_EXIST_MENU));

        //TODO : bulk update 적용
        categoryRepository.findBySuperMenu(from).stream()
                .forEach(category -> category.changeSuperMenu(to));
    }
}
