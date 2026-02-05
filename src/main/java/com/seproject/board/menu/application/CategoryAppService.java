package com.seproject.board.menu.application;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.admin.menu.utils.DelegatingMenuProvider;
import com.seproject.board.menu.controller.dto.CategoryResponse;
import com.seproject.board.menu.domain.model.Menu;
import com.seproject.board.menu.service.MenuService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CategoryAppService {
    private final MenuService menuService;
    private final DelegatingMenuProvider menuProvider;

    public CategoryResponse retrieveMenuById(Long menuId) {
        Menu targetMenu = menuService.findById(menuId);
//        retrieveSubMenu(targetMenu, res);
        Optional<Account> accountOptional = SecurityUtils.getAccount();
        List<Role> userRole = accountOptional.isPresent() ?
                accountOptional.get().getRoles() : Collections.EMPTY_LIST;

        CategoryResponse res = submenuSearch(targetMenu, userRole, targetMenu.getDepth());
        return res;
    }


    CategoryResponse submenuSearch(Menu menu , List<Role> userRole , int depth) {

        if (depth == 3) return null;

        CategoryResponse subMenuRes = new CategoryResponse(menu);
        List<Menu> subMenus = menuService.findSubMenu(menu.getMenuId()); //TODO : N+1, 테스트

        for (Menu subMenu : subMenus) {
            if (subMenu.accessible(userRole)) {
                CategoryResponse response = submenuSearch(subMenu, userRole, depth + 1);
                if (response != null) subMenuRes.addSubMenu(response);
            }
        }

        return subMenuRes;
    }

//    protected void retrieveSubMenu(Menu targetMenu, CategoryResponse res) {
//        if(targetMenu.getDepth()==0){
//
//            List<Menu> depth1menu = menuService.findSubMenu(targetMenu.getMenuId());
//
//            for(Menu menu : depth1menu) {
//
//                CategoryResponse subMenuRes = new CategoryResponse(menu);
//                List<Menu> depth2menu = menuRepository.findBySuperMenu(menu.getMenuId());
//
//                for(Menu menu2 : depth2menu){
//                    subMenuRes.addSubMenu(new CategoryResponse(menu2));
//                }
//
//                res.addSubMenu(subMenuRes);
//            }
//        } else if(targetMenu.getDepth() == 1) {
//
//            List<Menu> depth2menu = menuRepository.findBySuperMenu(targetMenu.getMenuId());
//
//            for(Menu menu : depth2menu) {
//                res.addSubMenu(new CategoryResponse(menu));
//            }
//        }
//    }

    public List<CategoryResponse> retrieveAllMenu() {
        //TODO : 3 depth의 경우 3 dpeth의 것 조회 불가능
        Optional<Account> account = SecurityUtils.getAccount();
        List<Role> roles = account.isPresent() ? account.get().getRoles() : List.of();

        List<Menu> menus = menuService.findByDepth(0);

        List<Long> menuIds = menus.stream()
                .map(Menu::getMenuId)
                .collect(Collectors.toList());

        Map<Long, List<Menu>> subMenuMap = menuService.findSubMenu(menuIds);
        List<CategoryResponse> categoryResponses = new ArrayList<>();

        for (Menu menu : menus) {
            if (menu.exposable(roles)) {
                List<Menu> subMenus = subMenuMap.getOrDefault(menu.getMenuId(), Collections.emptyList());

                CategoryResponse categoryResponse = new CategoryResponse(menu);

                subMenus.stream()
                        .map(CategoryResponse::new)
                        .forEach(categoryResponse::addSubMenu);

                categoryResponses.add(categoryResponse);
            }
        }

        return categoryResponses;
    }

}
