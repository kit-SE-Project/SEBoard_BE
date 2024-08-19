package com.seproject.admin.bulletin.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.bulletin.controller.dto.MainPageDTO.InternalSiteMenuResponse;
import com.seproject.admin.bulletin.controller.dto.MainPageDTO.MainPageMenuResponse;
import com.seproject.admin.dashboard.domain.DashBoardMenu;
import com.seproject.admin.dashboard.service.AdminDashBoardServiceImpl;
import com.seproject.board.bulletin.domain.model.MainPageMenu;
import com.seproject.board.bulletin.service.MainPageService;
import com.seproject.board.menu.domain.model.InternalSiteMenu;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class AdminMainPageAppService {

    private final MainPageService mainPageService;
    private final MenuService menuService;

    private final AdminDashBoardServiceImpl dashBoardService;

    //TODO : 추후 AOP로 변경 필요
    private void checkAuthorization(){
        Account account = SecurityUtils.getAccount()
                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));

        DashBoardMenu dashboardmenu = dashBoardService.findDashBoardMenuByUrl(DashBoardMenu.MAIN_PAGE_MENU_MANAGE_URL);

        if(!dashboardmenu.authorize(account.getRoles())){
            throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED, null);
        }
    }

    @Transactional
    public Long createMainPageMenu(Long menuId) {
        checkAuthorization();

        Menu menu = menuService.findById(menuId);
        Long mainPageMenuId = mainPageService.createMainPageMenu(menu);
        return mainPageMenuId;
    }

    @Transactional
    public void deleteMainPageMenu(Long id) {
        checkAuthorization();

        mainPageService.deleteMainPageMenu(id);
    }

    @Transactional
    public void updateMainPageMenu(List<Long> menuIds) {
        checkAuthorization();

        List<MainPageMenu> allMenus = mainPageService.findAll();
        mainPageService.deleteMainPageMenus(allMenus);

        List<Menu> menus = menuService.findByIds(menuIds);
        mainPageService.createMainPageMenu(menus);
    }

    public List<MainPageMenuResponse> retrieveAllMainPageMenus() {
        checkAuthorization();

        List<MainPageMenu> all = mainPageService.findAll();
        return all.stream().map(MainPageMenuResponse::toDTO)
                .collect(Collectors.toList());
    }

    public List<InternalSiteMenuResponse> retrieveAllInternalSiteMenu() {
        checkAuthorization();

        List<InternalSiteMenu> mainPageableMenu = mainPageService.findAllMainPageableMenu();
        return mainPageableMenu.stream()
                .map(InternalSiteMenuResponse::toDTO)
                .collect(Collectors.toList());
    }
}
