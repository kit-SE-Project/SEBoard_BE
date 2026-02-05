package com.seproject.admin.menu.controller;

import com.seproject.global.IntegrationTestSupport;

class AdminMenuControllerTest extends IntegrationTestSupport {
//    static final String url = "/admin/menu/";
//
//    @Test
//    public void 메뉴_하위_보드메뉴_생성() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        MenuAuthOption access = new MenuAuthOption();
//        access.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.OVER_USER.getName());
//
//        CreateMenuRequest request = getCreateMenuRequest(menu,new MenuAuthOption[]{access,expose,null,null});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "BOARD")
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush(); em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        List<Menu> subMenus = menuService.findSubMenu(menu.getMenuId());
//
//        assertEquals(1, subMenus.size());
//        Menu subMenu = subMenus.get(0);
//
//        assertEquals(subMenu.getSuperMenu().getMenuId(),request.getSuperCategoryId());
//        assertEquals(subMenu.getName(),request.getName());
//        assertEquals(subMenu.getDescription(),request.getDescription());
//        assertEquals(subMenu.getUrlInfo(),request.getUrlId());
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        assertTrue(subMenu.accessible(List.of(roleUser)));
//        assertTrue(subMenu.accessible(List.of(roleKumoh)));
//        assertTrue(subMenu.accessible(List.of(roleAdmin)));
//        assertFalse(subMenu.accessible(List.of(roleSetup.createRole())));
//
//        assertTrue(subMenu.exposable(List.of(roleUser)));
//        assertTrue(subMenu.exposable(List.of(roleKumoh)));
//        assertTrue(subMenu.exposable(List.of(roleAdmin)));
//        assertFalse(subMenu.exposable(List.of(roleSetup.createRole())));
//
//
//        List<Menu> categoryMenu = menuService.findSubMenu(subMenu.getMenuId());
//
//        assertEquals(categoryMenu.size(),1);
//    }
//    @Test
//    public void 메뉴_하위_메뉴_생성() throws Exception {
//        Menu groupMenu = menuSetup.createMenu();
//
//        MenuAuthOption access = new MenuAuthOption();
//        access.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.OVER_USER.getName());
//
//        CreateMenuRequest request = getCreateMenuRequest(groupMenu,new MenuAuthOption[]{access,expose,null,null});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "MENU")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        List<Menu> subMenus = menuService.findSubMenu(groupMenu.getMenuId());
//
//        assertEquals(1, subMenus.size());
//        Menu subMenu = subMenus.get(0);
//
//        assertEquals(subMenu.getSuperMenu().getMenuId(),request.getSuperCategoryId());
//        assertEquals(subMenu.getName(),request.getName());
//        assertEquals(subMenu.getDescription(),request.getDescription());
//        assertEquals(subMenu.getUrlInfo(),request.getUrlId());
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        assertTrue(subMenu.accessible(List.of(roleUser)));
//        assertTrue(subMenu.accessible(List.of(roleKumoh)));
//        assertTrue(subMenu.accessible(List.of(roleAdmin)));
//        assertFalse(subMenu.accessible(List.of(roleSetup.createRole())));
//
//        assertTrue(subMenu.exposable(List.of(roleUser)));
//        assertTrue(subMenu.exposable(List.of(roleKumoh)));
//        assertTrue(subMenu.exposable(List.of(roleAdmin)));
//        assertFalse(subMenu.exposable(List.of(roleSetup.createRole())));
//    }
//    @Test
//    public void 메뉴_하위_외부링크_생성() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.OVER_USER.getName());
//
//        CreateMenuRequest request =
//                getCreateMenuRequest(menu,new MenuAuthOption[]{null,expose,null,null});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "EXTERNAL")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        List<Menu> subMenus = menuService.findSubMenu(menu.getMenuId());
//
//        assertEquals(1, subMenus.size());
//        Menu subMenu = subMenus.get(0);
//
//        assertEquals(subMenu.getSuperMenu().getMenuId(),request.getSuperCategoryId());
//        assertEquals(subMenu.getName(),request.getName());
//        assertEquals(subMenu.getDescription(),request.getDescription());
//        assertEquals(subMenu.getUrlInfo(),request.getExternalUrl());
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        assertTrue(subMenu.exposable(List.of(roleUser)));
//        assertTrue(subMenu.exposable(List.of(roleKumoh)));
//        assertTrue(subMenu.exposable(List.of(roleAdmin)));
//        assertFalse(subMenu.exposable(List.of(roleSetup.createRole())));
//
//    }
//    @Test
//    public void 메뉴_하위_여러메뉴_생성() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        MenuAuthOption menuAccess = new MenuAuthOption();
//        menuAccess.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption menuExpose = new MenuAuthOption();
//        menuExpose.setOption(SelectOption.OVER_USER.getName());
//
//        CreateMenuRequest request =
//                getCreateMenuRequest(menu,new MenuAuthOption[]{menuAccess,menuExpose,null,null});
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "MENU")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        MenuAuthOption boardMenuAccess = new MenuAuthOption();
//        boardMenuAccess.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption boardMenuExpose = new MenuAuthOption();
//        boardMenuExpose.setOption(SelectOption.OVER_USER.getName());
//
//        request = getCreateMenuRequest(menu,
//                new MenuAuthOption[]{boardMenuAccess,boardMenuExpose,null,null});
//
//        perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "BOARD")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        MenuAuthOption externalMenuExpose = new MenuAuthOption();
//        externalMenuExpose.setOption(SelectOption.OVER_USER.getName());
//
//        request = getCreateMenuRequest(menu,
//                new MenuAuthOption[]{null,externalMenuExpose,null,null});
//
//        perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "EXTERNAL")
//                .content(objectMapper.writeValueAsString(request)));
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        List<Menu> subMenu = menuService.findSubMenu(menu.getMenuId());
//
//        assertEquals(subMenu.size(),3);
//    }
//    @Test
//    public void 메뉴_하위_카테고리_생성() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        MenuAuthOption manage = new MenuAuthOption();
//        manage.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption edit = new MenuAuthOption();
//        edit.setOption(SelectOption.OVER_USER.getName());
//
//        CreateMenuRequest request = getCreateMenuRequest(menu,
//                new MenuAuthOption[]{null,null,edit,manage});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "CATEGORY")
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush(); em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").value(ErrorCode.CATEGORY_CREATE_ERROR.getCode()));
//    }
//    @Test
//    public void 게시판메뉴_하위_카테고리_생성() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//
//        MenuAuthOption manage = new MenuAuthOption();
//        manage.setOption(SelectOption.ONLY_ADMIN.getName());
//        MenuAuthOption edit = new MenuAuthOption();
//        edit.setOption(SelectOption.ONLY_ADMIN.getName());
//
//        CreateMenuRequest request = getCreateMenuRequest(boardMenu,
//                new MenuAuthOption[]{null,null,edit,manage});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "CATEGORY")
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush(); em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isOk());
//
//        List<Menu> subMenus = menuService.findSubMenu(boardMenu.getMenuId());
//
//        assertEquals(1, subMenus.size());
//        Menu subMenu = subMenus.get(0);
//
//        assertEquals(subMenu.getSuperMenu().getMenuId(),request.getSuperCategoryId());
//        assertEquals(subMenu.getName(),request.getName());
//        assertEquals(subMenu.getDescription(),request.getDescription());
//        assertEquals(subMenu.getUrlInfo(),request.getUrlId());
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        assertTrue(subMenu.accessible(List.of(roleUser)));
//        assertTrue(subMenu.accessible(List.of(roleKumoh)));
//        assertTrue(subMenu.accessible(List.of(roleAdmin)));
//        assertTrue(subMenu.accessible(List.of(roleSetup.createRole())));
//
//        assertTrue(subMenu.exposable(List.of(roleUser)));
//        assertTrue(subMenu.exposable(List.of(roleKumoh)));
//        assertTrue(subMenu.exposable(List.of(roleAdmin)));
//        assertTrue(subMenu.exposable(List.of(roleSetup.createRole())));
//
//        assertFalse(subMenu.editable(List.of(roleUser)));
//        assertFalse(subMenu.editable(List.of(roleKumoh)));
//        assertTrue(subMenu.editable(List.of(roleAdmin)));
//        assertFalse(subMenu.editable(List.of(roleSetup.createRole())));
//
//        assertFalse(subMenu.manageable(List.of(roleUser)));
//        assertFalse(subMenu.manageable(List.of(roleKumoh)));
//        assertTrue(subMenu.manageable(List.of(roleAdmin)));
//        assertFalse(subMenu.manageable(List.of(roleSetup.createRole())));
//    }
//    @Test
//    public void depth_초과() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        BoardMenu lastDepthMenu = menuSetup.createBoardMenu(boardMenu);
//
//        MenuAuthOption manage = new MenuAuthOption();
//        manage.setOption(SelectOption.ONLY_ADMIN.getName());
//        MenuAuthOption edit = new MenuAuthOption();
//        edit.setOption(SelectOption.ONLY_ADMIN.getName());
//
//        CreateMenuRequest request = getCreateMenuRequest(lastDepthMenu,
//                new MenuAuthOption[]{null,null,edit,manage});
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .queryParam("categoryType", "CATEGORY")
//                .content(objectMapper.writeValueAsString(request)));
//
//        em.flush(); em.clear();
//
//        perform.andDo(print())
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").value(ErrorCode.MAX_DEPTH.getCode()));
//    }
//    @Test
//    public void 메뉴_하위_여러메뉴_생성_필수옵션_null() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//
//        MenuAuthOption option = new MenuAuthOption();
//        option.setOption(SelectOption.OVER_USER.getName());
//
//        MenuAuthOption[][] input = new MenuAuthOption[][]{
//                {option,null,null,null},
//                {null,option,null,null},
//                {null,null,option,null},
//                {null,null,null,option},
//        };
//
//        for (int i = 0; i < 2; i++) {
//            CreateMenuRequest request = getCreateMenuRequest(menu,input[i]);
//
//            mvc.perform(post(url)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .characterEncoding("UTF-8")
//                    .header("Authorization",accessToken)
//                    .queryParam("categoryType", "MENU")
//                    .content(objectMapper.writeValueAsString(request)))
//            .andExpect(status().isBadRequest())
//            .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MENU_REQUEST.getCode()));
//
//
//            mvc.perform(post(url)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .accept(MediaType.APPLICATION_JSON)
//                    .characterEncoding("UTF-8")
//                    .header("Authorization",accessToken)
//                    .queryParam("categoryType", "BOARD")
//                    .content(objectMapper.writeValueAsString(request)))
//            .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MENU_REQUEST.getCode()));
//
//            if (i != 1) {
//                mvc.perform(post(url)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                                .header("Authorization",accessToken)
//                                .queryParam("categoryType", "EXTERNAL")
//                                .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isBadRequest())
//                        .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MENU_REQUEST.getCode()));
//            }
//
//        }
//
//        input = new MenuAuthOption[][]{
//                {null,null,option,null},
//                {null,null,null,option},
//        };
//
//        for (int i = 0; i < 2; i++) {
//            CreateMenuRequest request = getCreateMenuRequest(boardMenu,input[i]);
//
//            mvc.perform(post(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .header("Authorization",accessToken)
//                            .queryParam("categoryType", "CATEGORY")
//                            .content(objectMapper.writeValueAsString(request)))
//                    .andExpect(status().isBadRequest())
//                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_MENU_REQUEST.getCode()));
//        }
//
//    }
//    @Test
//    public void 카테고리_조회() throws Exception {
//        Menu rootMenu1 = menuSetup.createMenu();
//
//        for (int i = 0; i < 3; i++) {
//            BoardMenu boardMenu = menuSetup.createBoardMenu(rootMenu1);
//            ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(rootMenu1);
//            for (int j = 0; j < 3; j++) {
//                menuSetup.createCategory(boardMenu);
//            }
//        }
//
//        Menu rootMenu2 = menuSetup.createMenu();
//
//        for (int i = 0; i < 2; i++) {
//            BoardMenu boardMenu = menuSetup.createBoardMenu(rootMenu2);
//            ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(rootMenu2);
//            for (int j = 0; j < 3; j++) {
//                menuSetup.createCategory(boardMenu);
//            }
//        }
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//        );
//
//        perform.andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0]").exists())
//                .andExpect(jsonPath("$[0].menuId").exists())
//                .andExpect(jsonPath("$[0].subMenus").isNotEmpty())
//                .andExpect(jsonPath("$[1]").exists())
//                .andExpect(jsonPath("$[1].menuId").exists())
//                .andExpect(jsonPath("$[1].subMenus").isNotEmpty());
//    }
//    @Test
//    public void 카테고리_상세조회() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//
//        for (int i = 0; i < 3; i++) {
//            menuSetup.createCategory(boardMenu);
//        }
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(get(url + "{categoryId}", boardMenu.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8"))
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        perform
//                .andExpect(jsonPath("$.menuId").value(boardMenu.getMenuId()))
//                .andExpect(jsonPath("$.name").value(boardMenu.getName()))
//                .andExpect(jsonPath("$.urlId").value(boardMenu.getUrlInfo()))
//                .andExpect(jsonPath("$.subMenus.size()").value(3));
//    }
//    @Test
//    public void 존재하지않는_카테고리_상세조회() throws Exception {
//        ResultActions perform = mvc.perform(get(url + "{categoryId}", 423432)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8"));
//
//        perform.andDo(print())
//                .andExpect(status().isNotFound());
//    }
//    @Test
//    public void 메뉴_수정() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        Role role = roleSetup.createRole();
//
//        MenuAuthOption access = new MenuAuthOption();
//        access.setOption(SelectOption.SELECT.getName());
//        access.setRoles(List.of(role.getId()));
//
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.SELECT.getName());
//        expose.setRoles(List.of(role.getId()));
//
//        UpdateMenuRequest request  = getUpdateMenuRequest(new MenuAuthOption[]{access,expose,null,null});
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(put(url + "{categoryId}", menu.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//        em.flush(); em.clear();
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        Menu findMenu = menuService.findById(menu.getMenuId());
//
//        List<MenuAuthorization> menuAuthorizations = findMenu.getMenuAuthorizations();
//        assertEquals(menuAuthorizations.size(),2);
//        assertEquals(findMenu.getName(),request.getName());
//        assertEquals(findMenu.getUrlInfo(),request.getUrlId());
//        assertEquals(findMenu.getDescription(),request.getDescription());
//
//        assertTrue(findMenu.accessible(List.of(role)));
//        assertFalse(findMenu.accessible(List.of(roleSetup.getRoleUser())));
//        assertFalse(findMenu.accessible(List.of(roleSetup.getRoleKumoh())));
//        assertFalse(findMenu.accessible(List.of(roleSetup.getRoleAdmin())));
//
//        assertTrue(findMenu.exposable(List.of(role)));
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleUser())));
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleKumoh())));
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleAdmin())));
//    }
//    @Test
//    public void 게시판메뉴_수정() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Role role = roleSetup.createRole();
//        Role kumoh = roleSetup.getRoleKumoh();
//        Role admin = roleSetup.getRoleAdmin();
//
//        MenuAuthOption access = new MenuAuthOption();
//        access.setOption(SelectOption.SELECT.getName());
//        access.setRoles(List.of(role.getId(),admin.getId()));
//
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.SELECT.getName());
//        expose.setRoles(List.of(role.getId(),admin.getId(), kumoh.getId()));
//
//        UpdateMenuRequest request  = getUpdateMenuRequest(new MenuAuthOption[]{access,expose,null,null});
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(put(url + "{categoryId}", boardMenu.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//        em.flush(); em.clear();
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        Menu findMenu = menuService.findById(boardMenu.getMenuId());
//
//        List<MenuAuthorization> menuAuthorizations = findMenu.getMenuAuthorizations();
//        assertEquals(menuAuthorizations.size(),2);
//        assertEquals(findMenu.getName(),request.getName());
//        assertEquals(findMenu.getUrlInfo(),request.getUrlId());
//        assertEquals(findMenu.getDescription(),request.getDescription());
//
//        assertTrue(findMenu.accessible(List.of(role)));
//        assertFalse(findMenu.accessible(List.of(roleSetup.getRoleUser())));
//        assertFalse(findMenu.accessible(List.of(roleSetup.getRoleKumoh())));
//        assertTrue(findMenu.accessible(List.of(admin)));
//
//        assertTrue(findMenu.exposable(List.of(role)));
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleUser())));
//        assertTrue(findMenu.exposable(List.of(kumoh)));
//        assertTrue(findMenu.exposable(List.of(admin)));
//    }
//
//    @Test
//    public void 외부메뉴_수정() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        MenuAuthOption expose = new MenuAuthOption();
//        expose.setOption(SelectOption.ONLY_ADMIN.getName());
//
//        UpdateMenuRequest request  = getUpdateMenuRequest(new MenuAuthOption[]{null,expose,null,null});
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(put(url + "{categoryId}", externalSiteMenu.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//        em.flush(); em.clear();
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        Menu findMenu = menuService.findById(externalSiteMenu.getMenuId());
//
//        List<MenuAuthorization> menuAuthorizations = findMenu.getMenuAuthorizations();
//        assertEquals(menuAuthorizations.size(),1);
//        assertEquals(findMenu.getName(),request.getName());
//        assertEquals(findMenu.getUrlInfo(),request.getExternalUrl());
//        assertEquals(findMenu.getDescription(),request.getDescription());
//
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleUser())));
//        assertFalse(findMenu.exposable(List.of(roleSetup.getRoleKumoh())));
//        assertTrue(findMenu.exposable(List.of(roleSetup.getRoleAdmin())));
//    }
//
//    @Test
//    public void 카테고리_수정() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Role roleUser = roleSetup.getRoleUser();
//        Role roleKumoh = roleSetup.getRoleKumoh();
//        Role roleAdmin = roleSetup.getRoleAdmin();
//
//        Role editRole = roleSetup.createRole();
//        MenuAuthOption edit = new MenuAuthOption();
//        edit.setOption(SelectOption.SELECT.getName());
//        edit.setRoles(List.of(roleKumoh.getId(),roleAdmin.getId(),editRole.getId()));
//
//        Role manageRole = roleSetup.createRole();
//        MenuAuthOption manage = new MenuAuthOption();
//        manage.setOption(SelectOption.SELECT.getName());
//        manage.setRoles(List.of(manageRole.getId(), roleUser.getId()));
//
//        UpdateMenuRequest request  = getUpdateMenuRequest(new MenuAuthOption[]{null,null,edit,manage});
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(put(url + "{categoryId}", category.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request)));
//        em.flush(); em.clear();
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk());
//
//        Menu findMenu = menuService.findById(category.getMenuId());
//
//        List<MenuAuthorization> menuAuthorizations = findMenu.getMenuAuthorizations();
//        assertEquals(menuAuthorizations.size(),2);
//
//        assertEquals(findMenu.getName(),request.getName());
//        assertEquals(findMenu.getUrlInfo(),request.getUrlId());
//        assertEquals(findMenu.getDescription(),request.getDescription());
//
//        assertFalse(findMenu.editable(List.of(roleSetup.getRoleUser())));
//        assertTrue(findMenu.editable(List.of(roleSetup.getRoleKumoh())));
//        assertTrue(findMenu.editable(List.of(roleSetup.getRoleAdmin())));
//        assertTrue(findMenu.editable(List.of(editRole)));
//        assertFalse(findMenu.editable(List.of(manageRole)));
//
//        assertTrue(findMenu.manageable(List.of(roleSetup.getRoleUser())));
//        assertFalse(findMenu.manageable(List.of(roleSetup.getRoleKumoh())));
//        assertFalse(findMenu.manageable(List.of(roleSetup.getRoleAdmin())));
//        assertFalse(findMenu.manageable(List.of(editRole)));
//        assertTrue(findMenu.manageable(List.of(manageRole)));
//    }
//
//    @Test
//    public void 메뉴_수정_필수옵션_없음() throws Exception {
//
//        Menu menu = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menu);
//        Category category = menuSetup.createCategory(boardMenu);
//
//        List<Menu> list = List.of(menu, boardMenu, externalSiteMenu, category);
//        MenuAuthOption menuAuthOption = new MenuAuthOption();
//        menuAuthOption.setOption(SelectOption.OVER_USER.getName());
//        MenuAuthOption[][][] input = new MenuAuthOption[][][]{
//                {
//                        {null,menuAuthOption,null,null},
//                        {menuAuthOption,null,null,null},
//                },
//                {
//                        {null,menuAuthOption,null,null},
//                        {menuAuthOption,null,null,null},
//                },
//                {
//                        {menuAuthOption,null,null,null},
//                },
//                {
//                        {null,null,menuAuthOption,null},
//                        {null,null,null,menuAuthOption},
//                },
//        };
//        em.flush(); em.clear();
//
//        for (int i = 0; i < 4; i++) {
//            Menu m = list.get(i);
//            for (int j = 0; j < input[i].length; j++) {
//                UpdateMenuRequest request = getUpdateMenuRequest(input[i][j]);
//
//                mvc.perform(put(url + "{categoryId}", m.getMenuId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(request)))
//                        .andExpect(status().isBadRequest());
//            }
//        }
//    }
//
//    @Test
//    public void 카테고리_삭제() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menu);
//        Category category = menuSetup.createCategory(boardMenu);
//
//        List<Menu> menus = List.of(category, externalSiteMenu, boardMenu, menu);
//
//        for (int i = 0; i < 4; i++) {
//            Menu m = menus.get(i);
//            em.flush(); em.clear();
//
//            ResultActions perform = mvc.perform(delete(url + "{categoryId}", m.getMenuId())
//                    .accept(MediaType.APPLICATION_JSON)
//                    .header("Authorization",accessToken)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .characterEncoding("UTF-8"));
//
//            perform.andExpect(status().isOk());
//
//            em.flush(); em.clear();
//
//            CustomIllegalArgumentException ex = assertThrows(CustomIllegalArgumentException.class, () -> {
//                menuService.findById(m.getMenuId());
//            });
//
//            assertEquals(ex.getErrorCode(),ErrorCode.NOT_EXIST_MENU);
//        }
//    }
//
//    @Test
//    public void 카테고리_삭제_실패() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//
//        em.flush(); em.clear();
//
//        ResultActions perform = mvc.perform(delete(url + "{categoryId}", menu.getMenuId())
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8"));
//
//        perform.andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.code").value(ErrorCode.CANNOT_DELETE_MENU.getCode()));
//    }
//
//
//    //TODO : migrate 테스트
}