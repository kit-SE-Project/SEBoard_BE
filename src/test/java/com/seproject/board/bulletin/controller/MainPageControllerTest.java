package com.seproject.board.bulletin.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class MainPageControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/mainPage/";
//
//    @Test
//    public void 메인_페이지_등록_게시물_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Menu root = menuSetup.createMenu();
//
//        BoardMenu boardMenu1 = menuSetup.createBoardMenu(root);
//        BoardMenu boardMenu2 = menuSetup.createBoardMenu(root);
//
//        Category category1 = menuSetup.createCategory(boardMenu1);
//        Category category2 = menuSetup.createCategory(boardMenu2);
//
//        MainPageMenu mainPageMenu1 = mainPageMenuSetup.createMainPageMenu(category1);
//        MainPageMenu mainPageMenu2 = mainPageMenuSetup.createMainPageMenu(category2);
//
//        for (int i = 0; i < 3; i++) {
//            postSetup.createPost(member,category1);
//        }
//
//        for (int i = 0; i < 3; i++) {
//            postSetup.createPost(member,category2);
//        }
//
//        em.flush(); em.clear();
//        ResultActions perform = mvc.perform(MockMvcRequestBuilders.get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//        );
//
//        perform
//                .andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.size()").value(2))
//                .andExpect(jsonPath("$[0].posts.content.size()").value(3))
//                .andExpect(jsonPath("$[1].posts.content.size()").value(3));
//    }
//
//
//
//
//
//
//
//
}