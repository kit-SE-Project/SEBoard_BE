package com.seproject.admin.menu.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.admin.menu.utils.DelegatingMenuProvider;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.seproject.admin.menu.controller.dto.MenuDTO.*;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AdminMenuAppService {

    private final AdminMenuService adminMenuService;
    private final MenuService menuService;
    private final DelegatingMenuProvider menuProvider;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.MENU_EDIT_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    @Transactional
    public void createMenu(CreateMenuRequest request, String categoryType) {
        checkAuthorization();

        String name = request.getName();
        if (name == null) {
            throw new CustomIllegalArgumentException(ErrorCode.MENU_NAME_NULL, null);
        }

        menuProvider.create(request, categoryType);
    }

    public List<MenuResponse> findAllMenuTree() {
        checkAuthorization();

        //TODO : 성능 개선?
        List<Menu> depth0Menus = menuService.findByDepth(0);
        List<MenuResponse> menuResponses = depth0Menus.stream()
                .map(
                        (v) -> new MenuResponse(
                                v, null,
                                v.getAvailableRoles().entrySet().stream()
                                        .collect(
                                                Collectors.toMap(
                                                        (entry) -> entry.getKey(),
                                                        (entry) -> Pair.of(
                                                                entry.getValue().getFirst().getName(),
                                                                entry.getValue().getSecond().stream()
                                                                        .map(role -> role.getName())
                                                                        .collect(Collectors.toList())
                                                        )
                                                )
                                        )
                        )
                )
                .collect(Collectors.toList());

        menuResponses.forEach(this::findAllMenuTree);

        return menuResponses;

        //depth 1까지 밖에 조회 불가능
//        Map<Menu, List<Menu>> menuListMap = menuService.findAllMenuTree();
//
//        List<MenuResponse> menuResponses = new ArrayList<>();
//        menuListMap.forEach((menu, subMenus) -> {
//            List<MenuResponse> subMenuResponses = subMenus.stream()
//                    .map((v) -> new MenuResponse(v, null))
//                    .collect(Collectors.toList());
//            menuResponses.add(new MenuResponse(menu,subMenuResponses));
//        });
    }

    private void findAllMenuTree(MenuResponse superMenus) {
        List<MenuResponse> subMenus = menuService.findSubMenu(superMenus.getMenuId()).stream()
                .map(
                        (v) -> new MenuResponse(
                                v, null,
                                v.getAvailableRoles().entrySet().stream()
                                        .collect(
                                                Collectors.toMap(
                                                        (entry) -> entry.getKey(),
                                                        (entry) -> Pair.of(
                                                                entry.getValue().getFirst().getName(),
                                                                entry.getValue().getSecond().stream()
                                                                        .map(role -> role.getName())
                                                                        .collect(Collectors.toList())
                                                        )
                                                )
                                        )
                        )
                )
                .collect(Collectors.toList());

        superMenus.setSubMenus(subMenus);

        subMenus.forEach(this::findAllMenuTree);
    }

    public MenuResponse findMenu(Long menuId) {
        checkAuthorization();

        Menu menu = menuService.findById(menuId);
        List<MenuResponse> subMenus = menuService.findSubMenu(menuId)
                .stream()
                .map((v) -> new MenuResponse(v, null))
                .collect(Collectors.toList());
        return new MenuResponse(menu, subMenus);
    }

    @Transactional
    public void delete(Long menuId) {
        checkAuthorization();

        adminMenuService.delete(menuId);
    }


    @Transactional
    public void migrateCategory(Long fromMenuId, Long toMenuId) {
        checkAuthorization();

        adminMenuService.changeSuperCategory(fromMenuId, toMenuId);
    }

    @Transactional
    public void update(Long categoryId, UpdateMenuRequest request) {
        checkAuthorization();

        Menu menu = menuService.findById(categoryId);

        menuProvider.update(menu, request);
    }
}
