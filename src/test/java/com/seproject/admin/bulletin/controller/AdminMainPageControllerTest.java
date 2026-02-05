package com.seproject.admin.bulletin.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminMainPageControllerTest extends IntegrationTestSupport {
//    static final String url = "/admin/mainPageMenus/";
//    @Test
//    public void 메인_페이지_메뉴_조회() throws Exception {
//        Menu rootMenu = menuSetup.createMenu();
//
//        for (int i = 0; i < 3; i++) {
//            BoardMenu boardMenu = menuSetup.createBoardMenu(rootMenu);
//            for (int j = 0; j < 3; j++) {
//                Category category = menuSetup.createCategory(boardMenu);
//                if (j == 1) {
//                    mainPageMenuSetup.createMainPageMenu(category);
//                }
//            }
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization", accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(3));
//    }
//    @Test
//    public void 메인_페이지_등록_가능_메뉴_조회() throws Exception {
//        Menu menu = menuSetup.createMenu();
//
//        for (int i = 0; i < 3; i++) {
//            BoardMenu boardMenu = menuSetup.createBoardMenu(menu);
//            for (int j = 0; j < 4; j++) {
//                Category category = menuSetup.createCategory(boardMenu);
//                if (j == 2) mainPageMenuSetup.createMainPageMenu(category);
//            }
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + "all")
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization", accessToken)
//                .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(15 - 3));
//    }
//
//    @Test
//    public void 메인_페이지_메뉴_추가() throws Exception {
//        BoardMenu boardMenu = menuSetup.createBoardMenu(menuSetup.createMenu());
//        Category category = menuSetup.createCategory(boardMenu);
//
//        CreateMainPageMenuRequest request = new CreateMainPageMenuRequest();
//        request.setMenuId(category.getMenuId());
//
//        em.flush();
//        em.clear();
//
//        mvc.perform(post(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        List<MainPageMenu> all = mainPageService.findAll();
//
//        assertEquals(all.size(),1);
//        MainPageMenu mainPageMenu = all.get(0);
//
//        assertEquals(mainPageMenu.getMenu().getMenuId(),category.getMenuId());
//    }
//
//    @Test
//    public void 메인_페이지_외부_메뉴는_등록_불가() throws Exception {
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menuSetup.createMenu());
//
//        CreateMainPageMenuRequest request = new CreateMainPageMenuRequest();
//        request.setMenuId(externalSiteMenu.getMenuId());
//
//        em.flush();
//        em.clear();
//
//        mvc.perform(post(url)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isBadRequest());
//    }
//
//
//    @Test
//    public void 등록할_수_없는_메뉴_메인페이지_추가() throws Exception {
//        Menu menu = menuSetup.createMenu();
//        ExternalSiteMenu externalSiteMenu = menuSetup.createExternalSiteMenu(menu);
//        em.flush();em.clear();
//
//        for (Menu m : List.of(menu,externalSiteMenu)) {
//            CreateMainPageMenuRequest request = new CreateMainPageMenuRequest();
//            request.setMenuId(m.getMenuId());
//
//            mvc.perform(post(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .header("Authorization",accessToken)
//                            .characterEncoding("UTF-8")
//                            .content(objectMapper.writeValueAsString(request))
//                    ).andDo(print())
//                    .andExpect(status().isBadRequest());
//        }
//    }
//
//    @Test
//    public void 메인_페이지_메뉴_삭제() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        MainPageMenu mainPageMenu = mainPageMenuSetup.createMainPageMenu(category);
//        em.flush(); em.clear();
//
//        DeleteMainPageMenuRequest request = new DeleteMainPageMenuRequest();
//        request.setId(mainPageMenu.getId());
//
//        mvc.perform(delete(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        CustomIllegalArgumentException ex = assertThrows(CustomIllegalArgumentException.class, () -> {
//            mainPageService.findById(mainPageMenu.getId());
//        });
//
//        assertEquals(ex.getErrorCode(), ErrorCode.NOT_EXIST_MAIN_PAGE_MENU);
//    }
//
//    @Test
//    public void 메인_페이지_수정() throws Exception {
//        Menu root = menuSetup.createMenu();
//
//        BoardMenu[] boardMenus = new BoardMenu[] {
//                menuSetup.createBoardMenu(root),
//                menuSetup.createBoardMenu(root),
//                menuSetup.createBoardMenu(root),
//        };
//
//        Category[][] categories = new Category[3][4];
//
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 4; j++) {
//                categories[i][j] = menuSetup.createCategory(boardMenus[i]);
//                if (j == 2) mainPageMenuSetup.createMainPageMenu(categories[i][j]);
//            }
//        }
//        em.flush(); em.clear();
//
//        List<MainPageMenu> now = mainPageService.findAll();
//        assertEquals(now.size(), 3);
//
//        List<Long> beforeUpdate = now.stream()
//                .map(MainPageMenu::getMenu)
//                .map(Menu::getMenuId)
//                .collect(Collectors.toList());
//
//        for (int i = 0; i < 3; i++) {
//            assertTrue(beforeUpdate.contains(categories[i][2].getMenuId()));
//        }
//
//        List<Long> menuIds = new ArrayList<>();
//
//        for (int i = 0; i < 3; i++) {
//            menuIds.add(categories[i][1].getMenuId());
//        }
//
//        UpdateMainPageMenuRequest request = new UpdateMainPageMenuRequest();
//        request.setMenuIds(menuIds);
//
//        em.flush(); em.clear();
//        mvc.perform(put(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .header("Authorization",accessToken)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//        em.flush(); em.clear();
//        List<MainPageMenu> updatedList = mainPageService.findAll();
//
//        assertEquals(updatedList.size(),3);
//
//        List<Long> ids = updatedList.stream()
//                .map(MainPageMenu::getMenu)
//                .map(Menu::getMenuId)
//                .collect(Collectors.toList());
//
//        for (int i = 0; i < 3; i++) {
//            assertTrue(ids.contains(menuIds.get(i)));
//        }
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
//
//
//
}