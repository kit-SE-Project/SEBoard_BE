//package com.seproject.board.menu.application;
//
//import com.seproject.account.role.domain.Role;
//import com.seproject.account.role.domain.RoleAuthorization;
//import com.seproject.account.authorization.domain.Authorization;
//import com.seproject.account.role.domain.repository.RoleRepository;
//import com.seproject.account.authorization.domain.repository.AuthorizationRepository;
//import com.seproject.account.authorization.domain.MenuAuthorization;
//import com.seproject.admin.domain.SelectOption;
//import com.seproject.admin.domain.repository.MenuExposeRepository;
//import com.seproject.account.role.service.RoleService;
//import com.seproject.error.errorCode.ErrorCode;
//import com.seproject.error.exception.CustomIllegalArgumentException;
//import com.seproject.board.menu.domain.model.Menu;
//import com.seproject.board.menu.domain.repository.MenuRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//
//@RequiredArgsConstructor
//@Transactional
//@Service
//public class AdminMenuService {
//
//    private final MenuRepository menuRepository;
//    private final RoleService roleService;
//    private final AuthorizationRepository authorizationRepository;
//    private final MenuExposeRepository menuExposeRepository;
//    private final RoleRepository roleRepository;
//
//    public CategoryAccessOptionResponse retrieve(Long menuId) {
//        Menu menu = menuRepository.findById(menuId).orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_CATEGORY, null));
//        List<MenuAuthorization> menuAuthorizations = menu.getMenuAuthorizations();
//        List<MenuAuthorization> write = new ArrayList<>();
//        List<MenuAuthorization> expose = new ArrayList<>();
//        List<MenuAuthorization> manage = new ArrayList<>();
//
//        for (MenuAuthorization menuAuthorization : menuAuthorizations) {
//            AccessOption accessOption = menuAuthorization.getAccessOption();
//            if(accessOption == AccessOption.WRITE) {
//                write.add(menuAuthorization);
//            } else if(accessOption == AccessOption.EXPOSE) {
//                expose.add(menuAuthorization);
//            } else if(accessOption == AccessOption.MANAGE) {
//                manage.add(menuAuthorization);
//            } else {
//                throw new RuntimeException("있을수 없는 일...");
//            }
//        }
//
//        Optional<Authorization> authorization = authorizationRepository.findByPath("/category/" + menuId + "/**");
//
//        MenuAuthorizationResponse writeResponse = MenuAuthorizationResponse.toDTO(write);
//        MenuAuthorizationResponse exposeResponse = MenuAuthorizationResponse.toDTO(expose);
//        MenuAuthorizationResponse manageResponse = MenuAuthorizationResponse.toDTO(manage);
//        Authorization auth;
//
//        if(authorization.isPresent()) {
//            auth = authorization.get();
//        } else {
//            auth = Authorization.builder()
//                    .path("/category/" + menuId + "/**")
//                    .selectOption(SelectOption.ALL)
//                    .roleAuthorizations(new ArrayList<>())
//                    .build();
//        }
//
//        List<RoleAuthorization> roleAuthorizations = auth.getRoleAuthorizations();
//
//        List<Role> roles = roleAuthorizations.stream().map(RoleAuthorization::getRole).collect(Collectors.toList());
//        SelectOption selectOption = auth.getSelectOption();
//        AccessResponse accessResponse = AccessResponse.toDTO(roles,selectOption);
//
//        return CategoryAccessOptionResponse.toDTO(menu,accessResponse,writeResponse,manageResponse,exposeResponse);
//    }
//
//    public void update(Long categoryId, CategoryAccessUpdateRequest request) {
//        Menu menu = menuRepository.findById(categoryId).orElseThrow(() -> new CustomIllegalArgumentException(ErrorCode.NOT_EXIST_CATEGORY, null));
//
//
//    }
//
//    public void update(Menu menu ,CategoryAccessUpdateRequestElement element, AccessOption accessOption) {
//        SelectOption selectOption = SelectOption.of(element.getOption());
//        List<Role> roles;
//
//        if(selectOption == SelectOption.SELECT) {
//            List<Long> roleIds = element.getRoles();
//            roles = roleRepository.findAllById(roleIds);
//        } else {
//            roles = roleService.convertRoles(selectOption);
//        }
//
//        update(menu,selectOption,accessOption,roles);
//    }
//
//    public void accessUpdate(Menu menu,CategoryAccessUpdateRequestElement access) {
//        SelectOption selectOption = SelectOption.of(access.getOption());
//        accessUpdate(menu,selectOption);
//    }
//
//    private void update(Menu menu , SelectOption selectOption, AccessOption accessOption,List<Role> roles) {
//        List<MenuAuthorization> collect = roles.stream().map(role ->
//                MenuAuthorization.builder()
//                        .menu(menu)
//                        .role(role)
//                        .accessOption(accessOption)
//                        .selectOption(selectOption)
//                        .build()).collect(Collectors.toList());
//
//        menu.updateMenuAuthorizations(collect);
//    }
//
//    private void accessUpdate(Menu menu,SelectOption selectOption) {
//        List<Role> roles = roleService.convertRoles(selectOption);
//        String path = "/category/" + menu.getMenuId() + "/**";
//
//        Optional<Authorization> optional = authorizationRepository.findByPath(path);
//
//        if(optional.isPresent()) {
//            Authorization authorization = optional.get();
//
//            List<RoleAuthorization> collect = roles.stream().map(role ->
//                    RoleAuthorization.builder()
//                            .role(role)
//                            .authorization(authorization)
//                            .build()).collect(Collectors.toList());
//
//            authorization.setRoleAuthorizations(collect);
//            authorization.setSelectOption(selectOption);
//
//            authorizationRepository.save(authorization);
//        } else {
//
//            Authorization authorization = Authorization.builder()
//                    .path(path)
//                    .build();
//
//            List<RoleAuthorization> collect = roles.stream().map(role ->
//                    RoleAuthorization.builder()
//                            .role(role)
//                            .authorization(authorization)
//                            .build()).collect(Collectors.toList());
//
//            authorization.setRoleAuthorizations(collect);
//            authorization.setSelectOption(selectOption);
//
//            authorizationRepository.save(authorization);
//        }
//    }
//
//    public void addExposeRole(Menu menu, List<Role> roles) {
//        Long menuId = menu.getMenuId();
//
//    }
//
//}
