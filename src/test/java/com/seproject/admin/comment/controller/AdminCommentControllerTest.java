package com.seproject.admin.comment.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class AdminCommentControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/admin/comments/";
//
//    @Test
//    public void 댓글_목록_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        for (int i = 0; i < 10; i++) {
//            commentSetup.createComment(post,member);
//        }
//
//        mvc.perform(get(url)
//                .contentType(MediaType.APPLICATION_JSON)
//                .accept(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(10));
//    }
//    @Test
//    public void 삭제_댓글_목록_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        for (int i = 0; i < 10; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            comment.delete(false);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(get(url + "deleted/")
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization", accessToken)
//                .param("page","0")
//                .param("perPage", "5")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(5))
//                .andExpect(jsonPath("$.totalElements").value(10));
//    }
//    @Test
//    public void 댓글_복구() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        Comment comment = commentSetup.createComment(post, member);
//        comment.delete(false);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                post(url + "{commentId}/restore",comment.getCommentId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        Comment findComment = commentService.findById(comment.getCommentId());
//
//        assertEquals(findComment.getStatus(), Status.NORMAL);
//    }
//    @Test
//    public void 없는_댓글_복구() throws Exception {
//        mvc.perform(
//                post(url + "{commentId}/restore",1234L)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//        ).andDo(print())
//        .andExpect(status().isNotFound());
//    }
//    @Test
//    public void 벌크_댓글_복구() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        List<Long> commentIds = new ArrayList<>();
//
//        for (int i = 0; i < 5; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            comment.delete(false);
//            commentIds.add(comment.getCommentId());
//        }
//
//        em.flush(); em.clear();
//
//        CommentDTO.BulkCommentRequest request = new CommentDTO.BulkCommentRequest();
//        request.setCommentIds(commentIds);
//
//        mvc.perform(
//                post(url + "restore/")
//                .accept(MediaType.APPLICATION_JSON)
//                .contentType(MediaType.APPLICATION_JSON)
//                .characterEncoding("UTF-8")
//                .header("Authorization",accessToken)
//                .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//        em.flush(); em.clear();
//
//        List<Comment> restoredComments = commentService.findAllByIds(commentIds);
//
//        for (Comment restoredComment : restoredComments) {
//            assertEquals(restoredComment.getStatus(),Status.NORMAL);
//        }
//    }
//    @Test
//    public void 벌크_없는_댓글_복구() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        List<Long> commentIds = new ArrayList<>();
//
//        for (int i = 0; i < 5; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            comment.delete(false);
//            commentIds.add(comment.getCommentId());
//        }
//        commentIds.add(32423L);
//
//        em.flush(); em.clear();
//
//        CommentDTO.BulkCommentRequest request = new CommentDTO.BulkCommentRequest();
//        request.setCommentIds(commentIds);
//
//        mvc.perform(
//                        post(url + "restore/")
//                                .accept(MediaType.APPLICATION_JSON)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                                .header("Authorization",accessToken)
//                                .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isNotFound());
//    }
//    @Test
//    public void 댓글_임시삭제() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        List<Long> commentIds = new ArrayList<>(4);
//        for (int i = 0; i < 4; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            commentIds.add(comment.getCommentId());
//        }
//
//        CommentDTO.BulkCommentRequest request = new CommentDTO.BulkCommentRequest();
//        request.setCommentIds(commentIds);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                delete(url)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//                .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//        System.out.println("--------------------------");
//        List<Comment> findComments = commentService.findAllByIds(commentIds);
//
//        for (Comment findComment : findComments) {
//            //TODO : fetch에 대한 생각 join 심함
//            assertEquals(findComment.getStatus(),Status.TEMP_DELETED);
//        }
//    }
//    @Test
//    public void 댓글_영구_삭제() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        List<Long> commentIds = new ArrayList<>(4);
//        for (int i = 0; i < 4; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            comment.delete(false);
//            commentIds.add(comment.getCommentId());
//        }
//
//        CommentDTO.BulkCommentRequest request = new CommentDTO.BulkCommentRequest();
//        request.setCommentIds(commentIds);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                delete(url + "permanent")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//                        .content(objectMapper.writeValueAsString(request))
//        ).andDo(print())
//        .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        List<Comment> deletedComments = commentService.findAllByIds(commentIds);
//
//        for (Comment deletedComment : deletedComments) {
//            assertEquals(deletedComment.getStatus(),Status.PERMANENT_DELETED);
//        }
//    }
//    @Test
//    public void 존재하지_않는_댓글_벌크_삭제() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        Member member = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(member, category);
//
//        List<Long> commentIds = new ArrayList<>(5);
//        for (int i = 0; i < 4; i++) {
//            Comment comment = commentSetup.createComment(post, member);
//            commentIds.add(comment.getCommentId());
//        }
//
//        commentIds.add(1234123L);
//
//        CommentDTO.BulkCommentRequest request = new CommentDTO.BulkCommentRequest();
//        request.setCommentIds(commentIds);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                        delete(url)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .header("Authorization",accessToken)
//                                .characterEncoding("UTF-8")
//                                .content(objectMapper.writeValueAsString(request))
//                ).andDo(print())
//                .andExpect(status().isNotFound());
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

}