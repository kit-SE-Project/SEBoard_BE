package com.seproject.admin.menu.service;

import com.seproject.account.role.domain.repository.RoleRepository;
import com.seproject.board.menu.domain.model.BoardMenu;
import com.seproject.board.menu.domain.model.Category;
import com.seproject.board.menu.domain.model.ExternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.domain.repository.BoardMenuRepository;
import com.seproject.board.menu.domain.repository.CategoryRepository;
import com.seproject.board.menu.domain.repository.ExternalSiteMenuRepository;
import com.seproject.board.menu.domain.repository.MenuRepository;
import com.seproject.board.post.domain.repository.PostRepository;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.InvalidDateException;
import com.seproject.error.exception.NoSuchResourceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdminMenuService {

    private final MenuRepository menuRepository;
    private final BoardMenuRepository boardMenuRepository;
    private final CategoryRepository categoryRepository;
    private final ExternalSiteMenuRepository externalSiteMenuRepository;
    private final PostRepository postRepository;
    
    private final RoleRepository roleRepository;

    private void addMenuAuthorization(Menu menu) {
//        Role adminRole = roleRepository.findByName(Role.ROLE_ADMIN)
//                .orElseThrow(() -> new NoSuchResourceException(ErrorCode.NOT_ENROLLED_ROLE));
//
//        MenuExposeAuthorization expose = new MenuExposeAuthorization(menu);
//        MenuManageAuthorization manage = new MenuManageAuthorization(menu);
//        MenuEditAuthorization edit = new MenuEditAuthorization(menu);
//        MenuAccessAuthorization access = new MenuAccessAuthorization(menu);
//
//        manage.update(List.of(adminRole));
//
//        menu.addAuthorization(expose);
//        menu.addAuthorization(manage);
//        menu.addAuthorization(edit);
//        menu.addAuthorization(access);
    }

    public void validUrlInfo(String urlInfo) {
        List<String> blackList = List.of("admin", "jenkins");

        if(menuRepository.existsByUrlInfo(urlInfo)) {
            throw new InvalidDateException(ErrorCode.INVALID_URL_INFO, "이미 사용중인 URL 입니다.");
        }

        if(blackList.contains(urlInfo)) {
            throw new InvalidDateException(ErrorCode.INVALID_URL_INFO, "사용할 수 없는 URL 입니다.");
        }
    }

    @Transactional
    public Long createMenu(Menu superMenu, String name, String description,String urlInfo) {
        if(!StringUtils.hasText(urlInfo)) {
            urlInfo = UUID.randomUUID().toString().substring(0,8);
        }

        validUrlInfo(urlInfo);

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

        validUrlInfo(urlInfo);

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

        validUrlInfo(urlInfo);

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

        if(menu.getType().equals("MENU") || menu.getType().equals("BOARD")){
            if(menuRepository.existsSubMenuById(menuId)){
                throw new CustomIllegalArgumentException(ErrorCode.CANNOT_DELETE_MENU,null);
            }

        }else if(menu.getType().equals("CATEGORY")){
            if(postRepository.existsByCategoryId(menu.getMenuId())){
                throw new CustomIllegalArgumentException(ErrorCode.CANNOT_DELETE_MENU,null);
            }
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
