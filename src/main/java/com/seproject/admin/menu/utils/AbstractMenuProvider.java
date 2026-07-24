package com.seproject.admin.menu.utils;

import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.service.RoleService;
import com.seproject.admin.menu.domain.SelectOption;
import com.seproject.admin.menu.service.AdminMenuService;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;

import java.util.List;

import static com.seproject.admin.menu.controller.dto.MenuDTO.*;

public abstract class AbstractMenuProvider {

     protected final AdminMenuService adminMenuService;
     protected final MenuService menuService;
     protected final RoleService roleService;
     protected final AuthorizationService authorizationService;

     public AbstractMenuProvider(AdminMenuService adminMenuService,
                                 MenuService menuService,
                                 RoleService roleService,
                                 AuthorizationService authorizationService) {

          this.adminMenuService = adminMenuService;
          this.menuService = menuService;
          this.roleService = roleService;
          this.authorizationService = authorizationService;
     }

     protected abstract boolean support(Menu menu);

     public abstract Long create(CreateMenuRequest request, String categoryType);
     public abstract Long update(Menu menu, UpdateMenuRequest request);

     public abstract MenuResponse toDto(Menu menu);

     protected List<Role> parseRoles(MenuAuthOption option) {
          SelectOption selectOption = SelectOption.of(option.getOption());

          if(selectOption == SelectOption.SELECT) {
               List<Long> roleIds = option.getRoles();

               Role adminRole = roleService.findByName(Role.ROLE_ADMIN);
               List<Role> roleList = roleService.findByIds(roleIds);

               if(!roleList.contains(adminRole)){
                    roleList.add(adminRole);
               }

               return roleList;
          }

          return roleService.convertRoles(selectOption);
     }
}
