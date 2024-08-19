package com.seproject.account.authorization.service;

import com.seproject.global.IntegrationTestSupport;

class AuthorizationServiceTest extends IntegrationTestSupport {
//
//    @Test
//    public void 인가_정보_조회() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        Long menuId = menu.getMenuId();
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(menuId);
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(menuId);
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(menuId);
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(menuId);
//        Assertions.assertEquals(accessAuthorization.getMenu(),menu);
//        Assertions.assertEquals(editAuthorization.getMenu(),menu);
//        Assertions.assertEquals(manageAuthorization.getMenu(),menu);
//        Assertions.assertEquals(exposeAuthorization.getMenu(),menu);
//    }
//
//    @Test
//    public void 접근_수정_ALL() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//
//        authorizationService.updateAccess(boardMenu, SelectOption.ALL,List.of());
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(boardMenu.getMenuId());
//        Assertions.assertEquals(accessAuthorization.getSelectOption(),SelectOption.ALL);
//        Assertions.assertEquals(accessAuthorization.getRoleAuthorizations().size(),0);
//    }
//
//    @Test
//    public void 접근_수정_테스트_SELECT() throws Exception {
//        Menu menu =menuSetup.createMenu();
//
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//
//        authorizationService.updateAccess(menu, SelectOption.SELECT,List.of(roleKumoh,roleUser));
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(menu.getMenuId());
//        Assertions.assertEquals(accessAuthorization.getSelectOption(),SelectOption.SELECT);
//        List<RoleAuthorization> roleAuthorizations = accessAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleUser));
//    }
//
//    @Test
//    public void 접근_수정_테스트_ONLY_ADMIN() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        authorizationService.updateAccess(menu, SelectOption.ONLY_ADMIN,List.of(roleAdmin));
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(menu.getMenuId());
//        Assertions.assertEquals(accessAuthorization.getSelectOption(),SelectOption.ONLY_ADMIN);
//        List<RoleAuthorization> roleAuthorizations = accessAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),1);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//    }
//
//    @Test
//    public void 접근_수정_테스트_OVER_USER() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//
//        authorizationService.updateAccess(menu, SelectOption.OVER_USER,List.of(roleAdmin,roleKumoh, roleUser));
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(menu.getMenuId());
//        Assertions.assertEquals(accessAuthorization.getSelectOption(),SelectOption.OVER_USER);
//        List<RoleAuthorization> roleAuthorizations = accessAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),3);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleUser));
//    }
//
//    @Test
//    public void 접근_수정_테스트_OVER_KUMOH() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        authorizationService.updateAccess(menu, SelectOption.OVER_KUMOH,List.of(roleAdmin,roleKumoh));
//
//        MenuAccessAuthorization accessAuthorization = authorizationService.findAccessAuthorization(menu.getMenuId());
//        Assertions.assertEquals(accessAuthorization.getSelectOption(),SelectOption.OVER_KUMOH);
//        List<RoleAuthorization> roleAuthorizations = accessAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//    }
//
//    @Test
//    public void 수정권한_수정_ALL() throws Exception {
//        Menu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        authorizationService.updateEdit(category,SelectOption.ALL,List.of());
//
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(category.getMenuId());
//        Assertions.assertEquals(editAuthorization.getMenu(),category);
//        Assertions.assertEquals(editAuthorization.getSelectOption(),SelectOption.ALL);
//
//        List<RoleAuthorization> roleAuthorizations = editAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),0);
//    }
//
//    @Test
//    public void 수정권한_수정_SELECT() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        authorizationService.updateEdit(category,SelectOption.SELECT,List.of(roleKumoh));
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(editAuthorization.getMenu(),category);
//        Assertions.assertEquals(editAuthorization.getSelectOption(),SelectOption.SELECT);
//
//        List<RoleAuthorization> roleAuthorizations = editAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size() , 1);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleKumoh));
//    }
//
//    @Test
//    public void 수정권한_수정_ONLY_ADMIN() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        authorizationService.updateEdit(category,SelectOption.ONLY_ADMIN,List.of(roleAdmin));
//
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(category.getMenuId());
//        Assertions.assertEquals(editAuthorization.getMenu(),category);
//        Assertions.assertEquals(editAuthorization.getSelectOption(),SelectOption.ONLY_ADMIN);
//
//        List<RoleAuthorization> roleAuthorizations = editAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),1);
//
//        List<Role> collect = roleAuthorizations.stream().map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertFalse(collect.contains(roleSetup.getRoleKumoh()));
//    }
//
//    @Test
//    public void 수정권한_수정_OVER_USER() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//        authorizationService.updateEdit(category,SelectOption.OVER_USER,List.of(roleAdmin,roleKumoh,roleUser));
//
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(category.getMenuId());
//        Assertions.assertEquals(editAuthorization.getMenu(),category);
//        Assertions.assertEquals(editAuthorization.getSelectOption(),SelectOption.OVER_USER);
//
//        List<RoleAuthorization> roleAuthorizations = editAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),3);
//
//        List<Role> collect = roleAuthorizations.stream().map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleUser));
//    }
//
//
//    @Test
//    public void 수정권한_수정_OVER_KUMOH() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//        authorizationService.updateEdit(category,SelectOption.OVER_KUMOH,List.of(roleAdmin,roleKumoh));
//
//        MenuEditAuthorization editAuthorization = authorizationService.findEditAuthorization(category.getMenuId());
//        Assertions.assertEquals(editAuthorization.getMenu(),category);
//        Assertions.assertEquals(editAuthorization.getSelectOption(),SelectOption.OVER_KUMOH);
//
//        List<RoleAuthorization> roleAuthorizations = editAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//
//        List<Role> collect = roleAuthorizations.stream().map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertFalse(collect.contains(roleUser));
//    }
//
//    @Test
//    public void 노출권한_수정_ALL() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        authorizationService.updateExpose(externalSiteMenu,SelectOption.ALL,List.of());
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(externalSiteMenu.getMenuId());
//
//        Assertions.assertEquals(exposeAuthorization.getMenu(),externalSiteMenu);
//        Assertions.assertEquals(exposeAuthorization.getSelectOption(),SelectOption.ALL);
//
//        List<RoleAuthorization> roleAuthorizations = exposeAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),0);
//    }
//
//    @Test
//    public void 노출권한_수정_SELECT() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        authorizationService.updateExpose(externalSiteMenu,SelectOption.SELECT,List.of(roleAdmin,roleKumoh));
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(externalSiteMenu.getMenuId());
//
//        Assertions.assertEquals(exposeAuthorization.getMenu(),externalSiteMenu);
//        Assertions.assertEquals(exposeAuthorization.getSelectOption(),SelectOption.SELECT);
//
//        List<RoleAuthorization> roleAuthorizations = exposeAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//    }
//
//    @Test
//    public void 노출권한_수정_OVER_USER() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleUser = roleSetup.getRoleUser();
//
//        authorizationService.updateExpose(externalSiteMenu,SelectOption.OVER_USER,List.of(roleAdmin,roleKumoh,roleUser));
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(externalSiteMenu.getMenuId());
//
//        Assertions.assertEquals(exposeAuthorization.getMenu(),externalSiteMenu);
//        Assertions.assertEquals(exposeAuthorization.getSelectOption(),SelectOption.OVER_USER);
//
//        List<RoleAuthorization> roleAuthorizations = exposeAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),3);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleUser));
//    }
//
//    @Test
//    public void 노출권한_수정_OVER_KUMOH() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//
//        authorizationService.updateExpose(externalSiteMenu,SelectOption.OVER_KUMOH,List.of(roleAdmin,roleKumoh));
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(externalSiteMenu.getMenuId());
//
//        Assertions.assertEquals(exposeAuthorization.getMenu(),externalSiteMenu);
//        Assertions.assertEquals(exposeAuthorization.getSelectOption(),SelectOption.OVER_KUMOH);
//
//        List<RoleAuthorization> roleAuthorizations = exposeAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//    }
//
//    @Test
//    public void 노출권한_수정_ONLY_ADMIN() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        authorizationService.updateExpose(externalSiteMenu,SelectOption.ONLY_ADMIN,List.of(roleAdmin));
//        MenuExposeAuthorization exposeAuthorization = authorizationService.findExposeAuthorization(externalSiteMenu.getMenuId());
//
//        Assertions.assertEquals(exposeAuthorization.getMenu(),externalSiteMenu);
//        Assertions.assertEquals(exposeAuthorization.getSelectOption(),SelectOption.ONLY_ADMIN);
//
//        List<RoleAuthorization> roleAuthorizations = exposeAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),1);
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(roleAdmin));
//    }
//
//
//    @Test
//    public void 관리권한_수정_ALL() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        authorizationService.updateManage(category,SelectOption.ALL,List.of());
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(manageAuthorization.getMenu(),category);
//        Assertions.assertEquals(manageAuthorization.getSelectOption(),SelectOption.ALL);
//        List<RoleAuthorization> roleAuthorizations = manageAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),0);
//    }
//
//    @Test
//    public void 관리권한_수정_SELECT() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role role1 = roleSetup.createRole();
//        Role role2 = roleSetup.createRole();
//        Role role3 = roleSetup.createRole();
//
//        authorizationService.updateManage(category,SelectOption.SELECT,List.of(role1,role2,role3));
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(manageAuthorization.getMenu(),category);
//        Assertions.assertEquals(manageAuthorization.getSelectOption(),SelectOption.SELECT);
//        List<RoleAuthorization> roleAuthorizations = manageAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),3);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//        Assertions.assertTrue(collect.contains(role1));
//        Assertions.assertTrue(collect.contains(role2));
//        Assertions.assertTrue(collect.contains(role3));
//        Assertions.assertFalse(collect.contains(roleSetup.createRole()));
//    }
//
//    @Test
//    public void 관리권한_수정_OVER_USER() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        authorizationService.updateManage(category,SelectOption.OVER_USER,List.of(roleUser,roleKumoh,roleAdmin));
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(manageAuthorization.getMenu(),category);
//        Assertions.assertEquals(manageAuthorization.getSelectOption(),SelectOption.OVER_USER);
//        List<RoleAuthorization> roleAuthorizations = manageAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),3);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//
//        Assertions.assertTrue(collect.contains(roleUser));
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleAdmin));
//    }
//
//    @Test
//    public void 관리권한_수정_OVER_KUMOH() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        authorizationService.updateManage(category,SelectOption.OVER_KUMOH,List.of(roleKumoh,roleAdmin));
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(manageAuthorization.getMenu(),category);
//        Assertions.assertEquals(manageAuthorization.getSelectOption(),SelectOption.OVER_KUMOH);
//        List<RoleAuthorization> roleAuthorizations = manageAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),2);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//
//        Assertions.assertTrue(collect.contains(roleKumoh));
//        Assertions.assertTrue(collect.contains(roleAdmin));
//    }
//
//    @Test
//    public void 관리권한_수정_ONLY_ADMIN() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        authorizationService.updateManage(category,SelectOption.ONLY_ADMIN,List.of(roleAdmin));
//        MenuManageAuthorization manageAuthorization = authorizationService.findManageAuthorization(category.getMenuId());
//
//        Assertions.assertEquals(manageAuthorization.getMenu(),category);
//        Assertions.assertEquals(manageAuthorization.getSelectOption(),SelectOption.ONLY_ADMIN);
//        List<RoleAuthorization> roleAuthorizations = manageAuthorization.getRoleAuthorizations();
//        Assertions.assertEquals(roleAuthorizations.size(),1);
//
//        List<Role> collect = roleAuthorizations.stream()
//                .map(RoleAuthorization::getRole)
//                .collect(Collectors.toList());
//
//        Assertions.assertTrue(collect.contains(roleAdmin));
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
}