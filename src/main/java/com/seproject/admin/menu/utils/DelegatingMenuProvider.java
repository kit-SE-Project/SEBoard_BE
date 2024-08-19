package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.controller.dto.MenuDTO;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DelegatingMenuProvider extends AbstractMenuProvider {

    private final List<AbstractMenuProvider> providers;

    public DelegatingMenuProvider(AdminMenuService adminMenuService, MenuService menuService, RoleService roleService, AuthorizationService authorizationService) {

        super(adminMenuService, menuService, roleService, authorizationService);
        providers = List.of(
                new CategoryProvider(adminMenuService,menuService,roleService,authorizationService),
                new BoardMenuProvider(adminMenuService,menuService,roleService,authorizationService),
                new ExternalSiteMenuProvider(adminMenuService,menuService,roleService,authorizationService),
                new MenuProvider(adminMenuService,menuService,roleService,authorizationService)
        );
    }

    @Override
    protected boolean support(Menu menu) {
        return false;
    }

    @Override
    public Long create(MenuDTO.CreateMenuRequest request, String categoryType) {
        for (AbstractMenuProvider provider : providers) {
            Long id = provider.create(request, categoryType);
            if (id != null) return id;
        }

        throw new CustomIllegalArgumentException(ErrorCode.ILLEGAL_MENU_TYPE,null);
    }

    @Override
    public Long update(Menu menu, MenuDTO.UpdateMenuRequest request) {
        for (AbstractMenuProvider provider : providers) {
            Long id = provider.update(menu,request);
            if (id != null) return id;
        }

        throw new CustomIllegalArgumentException(ErrorCode.ILLEGAL_MENU_TYPE, null);
    }

    @Override
    public MenuDTO.MenuResponse toDto(Menu menu) {
        for (AbstractMenuProvider provider : providers) {
            MenuDTO.MenuResponse dto = provider.toDto(menu);
            if (dto != null) return dto;
        }
        return null;
    }
}
