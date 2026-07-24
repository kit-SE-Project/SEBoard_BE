package com.seproject.board.common.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class ReportControllerTest extends IntegrationTestSupport {
//
//
//
//    @Test
//    public void post_신고_임계치_설정() throws Exception {
//        ReportThresholdRequest request = new ReportThresholdRequest();
//        request.setThreshold(22);
//        request.setThresholdType("POST");
//
//        mvc.perform(
//                MockMvcRequestBuilders.post("/report/threshold/")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        Optional<ReportThreshold> postThreshold = reportThresholdRepository.findPostThreshold();
//        assertEquals(postThreshold.get().getThreshold(),22);
//    }
//
//    @Test
//    public void comment_신고_임계치_설정() throws Exception {
//        ReportThresholdRequest request = new ReportThresholdRequest();
//        request.setThreshold(22);
//        request.setThresholdType("COMMENT");
//
//        mvc.perform(
//                        MockMvcRequestBuilders.post("/report/threshold/")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                                .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        Optional<ReportThreshold> postThreshold = reportThresholdRepository.findCommentThreshold();
//        assertEquals(postThreshold.get().getThreshold(),22);
//    }
//
//    @Test
//    public void 게시글_신고() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//
//        em.flush(); em.clear();
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        mvc.perform(
//                MockMvcRequestBuilders.get("/posts/{postId}/report",post.getPostId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Post findPost = postService.findById(post.getPostId());
//        assertEquals(findPost.getReportCount(),1);
//    }
//
//    @Test
//    public void 댓글_신고() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post,member);
//
//        em.flush(); em.clear();
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        mvc.perform(
//                        MockMvcRequestBuilders.get("/comments/{commentId}/report", comment.getCommentId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Comment findComment = commentService.findById(comment.getCommentId());
//        assertEquals(findComment.getReportCount(),1);
//    }
//
//
//    @Test
//    public void 게시글_임계치_초과() throws Exception {
//        ReportThresholdRequest request = new ReportThresholdRequest();
//        request.setThreshold(0);
//        request.setThresholdType("POST");
//
//        mvc.perform(
//                        MockMvcRequestBuilders.post("/report/threshold/")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                                .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        Optional<ReportThreshold> postThreshold = reportThresholdRepository.findPostThreshold();
//        assertEquals(postThreshold.get().getThreshold(),0);
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//
//        em.flush(); em.clear();
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        mvc.perform(
//                        MockMvcRequestBuilders.get("/posts/{postId}/report",post.getPostId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Post findPost = postService.findById(post.getPostId());
//        assertEquals(findPost.getReportCount(),1);
//        assertEquals(findPost.getStatus(), Status.REPORTED);
//    }
//
//    @Test
//    public void 댓글_임계치_초과() throws Exception {
//
//        ReportThresholdRequest request = new ReportThresholdRequest();
//        request.setThreshold(0);
//        request.setThresholdType("COMMENT");
//
//        mvc.perform(
//                        MockMvcRequestBuilders.post("/report/threshold/")
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                                .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        Optional<ReportThreshold> postThreshold = reportThresholdRepository.findCommentThreshold();
//        assertEquals(postThreshold.get().getThreshold(),0);
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post,member);
//
//        em.flush(); em.clear();
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        mvc.perform(
//                        MockMvcRequestBuilders.get("/comments/{commentId}/report", comment.getCommentId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Comment findComment = commentService.findById(comment.getCommentId());
//        assertEquals(findComment.getReportCount(),1);
//        assertEquals(findComment.getStatus(),Status.REPORTED);
//
//    }
//
//
//
//
//
//
}