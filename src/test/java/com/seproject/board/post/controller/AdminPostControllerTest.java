package com.seproject.board.post.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminPostControllerTest extends IntegrationTestSupport {
//
//
//    static final String url = "/admin/posts/";
//
//    @Test
//    public void 게시글_목록_조회_테스트() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        for (int i = 0; i < 33; i++) {
//            postSetup.createPost(member, category);
//        }
//
//        em.flush();
//        em.clear();
//
//        mvc.perform(get(url)
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .queryParam("page","1")
//                .queryParam("perPage","12")
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(12))
//                .andExpect(jsonPath("$.empty").value(false))
//                .andExpect(jsonPath("$.first").value(false))
//                .andExpect(jsonPath("$.last").value(false))
//                .andExpect(jsonPath("$.number").value(1))
//                .andExpect(jsonPath("$.numberOfElements").value(12))
//                .andExpect(jsonPath("$.size").value(12));
//    }
//
//    @Test
//    public void 게시글_복구_테스트() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//        post.increaseReportCount(ReportThreshold.of(5, "POST"));
//        post.delete(true);
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + post.getPostId() + "/restore")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//
//        Post findPost = postService.findById(post.getPostId());
//
//        assertEquals(findPost.getStatus(), Status.NORMAL);
//        assertEquals(findPost.getReportCount(),0);
//    }
//
//    @Test
//    public void 게시글_대량_복구_테스트() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        List<Long> postIds = new ArrayList<>();
//        for (int i = 0; i < 5; i++) {
//            Post post = postSetup.createPost(member, category);
//            post.increaseReportCount(ReportThreshold.of(5, "POST"));
//            post.delete(true);
//
//            postIds.add(post.getPostId());
//        }
//
//        em.flush(); em.clear();
//
//        PostRequest.BulkPostRequest request = new PostRequest.BulkPostRequest();
//        request.setPostIds(postIds);
//
//        mvc.perform(post(url + "restore")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        for (Long postId : postIds) {
//            Post findPost = postService.findById(postId);
//
//            assertEquals(findPost.getStatus(), Status.NORMAL);
//            assertEquals(findPost.getReportCount(),0);
//        }
//    }
//
//    @Test
//    public void 게시글_임시_삭제_성공() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        List<Long> postIds = new ArrayList<>();
//
//        for (int i = 0; i < 4; i++) {
//            Post post = postSetup.createPost(member, category);
//            postIds.add(post.getPostId());
//        }
//
//        em.flush(); em.clear();
//
//        PostRequest.BulkPostRequest request = new PostRequest.BulkPostRequest();
//        request.setPostIds(postIds);
//
//        mvc.perform(delete(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .content(objectMapper.writeValueAsString(request))
//                .header("Authorization",accessToken)
//        ).andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        em.flush(); em.clear();
//
//        for (Long postId : postIds) {
//            Post findPost = postService.findById(postId);
//            assertEquals(findPost.getStatus(),Status.TEMP_DELETED);
//        }
//    }
//
//    @Test
//    public void 게시글_영구_삭제_성공() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        List<Long> postIds = new ArrayList<>();
//
//        for (int i = 0; i < 4; i++) {
//            Post post = postSetup.createPost(member, category);
//            postIds.add(post.getPostId());
//        }
//
//        em.flush(); em.clear();
//
//        PostRequest.BulkPostRequest request = new PostRequest.BulkPostRequest();
//        request.setPostIds(postIds);
//
//        mvc.perform(delete(url + "permanent")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//                        .header("Authorization",accessToken)
//                ).andDo(MockMvcResultHandlers.print())
//                .andExpect(MockMvcResultMatchers.status().isOk());
//
//        em.flush(); em.clear();
//
//        for (Long postId : postIds) {
//            Post findPost = postService.findById(postId);
//            assertEquals(findPost.getStatus(),Status.PERMANENT_DELETED);
//        }
//    }
//
//    @Test
//    public void 삭제된_게시글_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        for (int i = 0; i < 36; i++) {
//            Post post = postSetup.createPost(member, category);
//            post.delete(false);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + "deleted")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .queryParam("page","1")
//                .queryParam("perPage","24")
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(12))
//                .andExpect(jsonPath("$.empty").value(false))
//                .andExpect(jsonPath("$.first").value(false))
//                .andExpect(jsonPath("$.last").value(true))
//                .andExpect(jsonPath("$.number").value(1))
//                .andExpect(jsonPath("$.totalElements").value(36))
//                .andExpect(jsonPath("$.numberOfElements").value(12))
//                .andExpect(jsonPath("$.size").value(24));
//    }
//
//    @Test
//    public void 게시물_카테고리_이동() throws Exception {
//        Category fromCategory = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Category toCategory = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        List<Long> postIds = new ArrayList<>();
//        for (int i = 0; i < 36; i++) {
//            Post post = postSetup.createPost(member, fromCategory);
//            postIds.add(post.getPostId());
//        }
//
//        PostRequest.MigratePostRequest request = new PostRequest.MigratePostRequest();
//        request.setFromCategoryId(fromCategory.getMenuId());
//        request.setToCategoryId(toCategory.getMenuId());
//
//        em.flush(); em.clear();
//
//        mvc.perform(post(url + "migrate")
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        for (Long postId : postIds) {
//            Post findPost = postService.findById(postId);
//            Category category = findPost.getCategory();
//            assertEquals(category.getMenuId(),toCategory.getMenuId());
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
}