package com.seproject.board.menu.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class CategoryControllerTest extends IntegrationTestSupport {
//
//
//    static final String url = "/menu/";
//
//    @Test
//    public void 메뉴_목록_조회() throws Exception {
//        List<Menu> rootMenus = new ArrayList<>();
//
//        Map<Long,Integer> map = new HashMap<>();
//
//        List<Integer> boardMenusSize = List.of(3,2,4);
//
//        List<List<Integer>> categorySize = List.of(
//                List.of(2,2,4),
//                List.of(4,3),
//                List.of(6,5,4,3)
//        );
//
//        for (int i = 0; i < 3; i++) {
//            Menu root = menuSetup.createMenu();
//            rootMenus.add(root);
//
//            for (int j = 0; j < boardMenusSize.get(i); j++) {
//                BoardMenu boardMenu = menuSetup.createBoardMenu(root);
//
//                for (int k = 0; k < categorySize.get(i).get(j); k++) {
//                    Category category = menuSetup.createCategory(boardMenu);
//                }
//
//                em.flush(); em.clear();
//
//                Long menuId = boardMenu.getMenuId();
//                map.put(menuId,categorySize.get(i).get(j));
//            }
//
//            map.put(root.getMenuId(),boardMenusSize.get(i));
//        }
//
//        mvc.perform(
//                MockMvcRequestBuilders.get(url)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(3));
//    }
//
//    @Test
//    public void 하위_카테고리_조회() throws Exception {
//        List<Menu> rootMenus = new ArrayList<>();
//
//        Map<Long,Integer> map = new HashMap<>();
//
//        List<Integer> boardMenusSize = List.of(3,2,4);
//
//        List<List<Integer>> categorySize = List.of(
//                List.of(2,2,4),
//                List.of(4,3),
//                List.of(6,5,4,3)
//        );
//
//        for (int i = 0; i < 3; i++) {
//            Menu root = menuSetup.createMenu();
//            rootMenus.add(root);
//
//            for (int j = 0; j < boardMenusSize.get(i); j++) {
//                BoardMenu boardMenu = menuSetup.createBoardMenu(root);
//
//                for (int k = 0; k < categorySize.get(i).get(j); k++) {
//                    Category category = menuSetup.createCategory(boardMenu);
//                }
//
//                em.flush(); em.clear();
//
//                Long menuId = boardMenu.getMenuId();
//                map.put(menuId,categorySize.get(i).get(j));
//            }
//
//            map.put(root.getMenuId(),boardMenusSize.get(i));
//        }
//
//        Menu targetMenu = rootMenus.get(1);
//
//        mvc.perform(
//                    MockMvcRequestBuilders.get(url + "{menuId}", targetMenu.getMenuId())
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .characterEncoding("UTF-8")
//                            .header("Authorization",accessToken)
//
//            ).andDo(print())
//            .andExpect(status().isOk())
//            .andExpect(jsonPath("$.subMenu.size()").value(2))
//            .andExpect(jsonPath("$.menuId").value(targetMenu.getMenuId()))
//            .andExpect(jsonPath("$.name").value(targetMenu.getName()))
//            .andExpect(jsonPath("$.urlId").value(targetMenu.getUrlInfo()))
//            .andExpect(jsonPath("$.externalUrl").isEmpty())
//            .andExpect(jsonPath("$.type").value("MENU"));
//
//
//    }
//
}