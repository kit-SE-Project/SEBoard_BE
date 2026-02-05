package com.seproject.board.comment.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

class ReplyControllerTest extends IntegrationTestSupport {
//    static final String url = "/reply/";
//
//    @Test
//    public void Named_답글_작성() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member postWriter = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(postWriter, category);
//
//        FormAccount commentWriterAccount = accountSetup.createFormAccount();
//        Anonymous commentWriter = boardUserSetup.createAnonymous(commentWriterAccount);
//
//        FormAccount replyWriterAccount = accountSetup.createFormAccount();
//        Anonymous replyWriter = boardUserSetup.createAnonymous(replyWriterAccount);
//
//        Comment superComment = commentSetup.createComment(post,commentWriter);
//        Comment tagComment = replySetup.createReply(post,replyWriter,superComment,superComment);
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        CreateReplyRequest request = new CreateReplyRequest();
//
//        request.setPostId(post.getPostId());
//        request.setSuperCommentId(superComment.getCommentId());
//        request.setTagCommentId(tagComment.getCommentId());
//        request.setAnonymous(false);
//        request.setReadOnlyAuthor(false);
//        request.setContents(UUID.randomUUID().toString());
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                    post(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .header("Authorization",accessToken)
//                            .content(objectMapper.writeValueAsString(request))
//                            .characterEncoding("UTF-8")
//            ).andDo(print())
//            .andExpect(status().isCreated());
//
//        em.flush(); em.clear();
//
//        List<Reply> bySuperCommendId = replyService.findBySuperCommendId(superComment.getCommentId());
//
//        assertEquals(bySuperCommendId.size(),2);
//    }
//    @Test
//    public void Unnamed_답글_작성() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Anonymous postWriter = boardUserSetup.createAnonymous(formAccount);
//        Post post = postSetup.createPost(postWriter, category);
//
//        FormAccount commentWriterAccount = accountSetup.createFormAccount();
//        Anonymous commentWriter = boardUserSetup.createAnonymous(commentWriterAccount);
//
//        Comment superComment = commentSetup.createComment(post,commentWriter);
//
//        FormAccount replyWriteAccount = accountSetup.createFormAccount();
//        String accessToken = tokenSetup.getAccessToken(replyWriteAccount);
//
//        CreateReplyRequest request = new CreateReplyRequest();
//
//        request.setPostId(post.getPostId());
//        request.setSuperCommentId(superComment.getCommentId());
//        request.setTagCommentId(superComment.getCommentId());
//        request.setAnonymous(true);
//        request.setReadOnlyAuthor(false);
//        request.setContents(UUID.randomUUID().toString());
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                    post(url)
//                            .contentType(MediaType.APPLICATION_JSON)
//                            .accept(MediaType.APPLICATION_JSON)
//                            .header("Authorization",accessToken)
//                            .content(objectMapper.writeValueAsString(request))
//                            .characterEncoding("UTF-8")
//            ).andDo(print())
//            .andExpect(status().isCreated());
//
//        em.flush(); em.clear();
//
//        List<Reply> bySuperCommendId = replyService.findBySuperCommendId(superComment.getCommentId());
//
//        assertEquals(bySuperCommendId.size(),1);
//        Reply reply = bySuperCommendId.get(0);
//
//        BoardUser author = reply.getAuthor();
//        assertTrue(author.isAnonymous());
//        assertEquals(author.getName(),"익명1");
//    }
//    @Test
//    public void 익명i_테스트() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Anonymous postWriter = boardUserSetup.createAnonymous(formAccount);
//        Post post = postSetup.createPost(postWriter, category);
//
//        FormAccount commentWriterAccount = accountSetup.createFormAccount();
//        Anonymous commentWriter = boardUserSetup.createAnonymous(commentWriterAccount);
//
//        Comment superComment = commentSetup.createComment(post,commentWriter);
//
//        CreateReplyRequest request = new CreateReplyRequest();
//
//        request.setPostId(post.getPostId());
//        request.setSuperCommentId(superComment.getCommentId());
//        request.setTagCommentId(superComment.getCommentId());
//        request.setAnonymous(true);
//        request.setReadOnlyAuthor(false);
//        request.setContents(UUID.randomUUID().toString());
//
//        for (int i = 1; i <= 5; i++) {
//            FormAccount replyWriteAccount = accountSetup.createFormAccount();
//            String accessToken = tokenSetup.getAccessToken(replyWriteAccount);
//
//            em.flush(); em.clear();
//
//            MvcResult mvcResult = mvc.perform(
//                            post(url)
//                                    .contentType(MediaType.APPLICATION_JSON)
//                                    .accept(MediaType.APPLICATION_JSON)
//                                    .header("Authorization", accessToken)
//                                    .content(objectMapper.writeValueAsString(request))
//                                    .characterEncoding("UTF-8")
//                    ).andDo(print())
//                    .andExpect(status().isCreated())
//                    .andReturn();
//
//            String json = mvcResult.getResponse().getContentAsString();
//            long id = (int) JsonPath.parse(json).read("$.id");
//            em.flush(); em.clear();
//
//            Reply reply = replyService.findById(id);
//
//            BoardUser author = reply.getAuthor();
//            assertTrue(author.isAnonymous());
//            assertEquals(author.getName(), "익명" + i);
//        }
//
//
//
//
//
//
//
//
//
//    }
//    @Test
//    public void 답글_수정() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member postWriter = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(postWriter, category);
//
//        FormAccount commentWriterAccount = accountSetup.createFormAccount();
//        Anonymous commentWriter = boardUserSetup.createAnonymous(commentWriterAccount);
//        Comment superComment = commentSetup.createComment(post,commentWriter);
//
//        FormAccount replyWriterAccount = accountSetup.createFormAccount();
//        Anonymous replyWriter = boardUserSetup.createAnonymous(replyWriterAccount);
//
//        Reply reply = replySetup.createReply(post,replyWriter,superComment,superComment);
//
//        String accessToken = tokenSetup.getAccessToken(replyWriterAccount);
//
//        UpdateReplyRequest request = new UpdateReplyRequest();
//        request.setContents(UUID.randomUUID().toString());
//        request.setReadOnlyAuthor(false);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                put(url + "{replyId}",reply.getCommentId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .content(objectMapper.writeValueAsString(request))
//                        .characterEncoding("UTF-8")
//            ).andDo(print())
//            .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Reply byId = replyService.findById(reply.getCommentId());
//
//        assertEquals(byId.getContents(),request.getContents());
//        assertEquals(byId.isOnlyReadByAuthor(),request.isReadOnlyAuthor());
//    }
//    @Test
//    public void 답글_삭제() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member postWriter = boardUserSetup.createMember(formAccount);
//        Post post = postSetup.createPost(postWriter, category);
//
//        FormAccount commentWriterAccount = accountSetup.createFormAccount();
//        Anonymous commentWriter = boardUserSetup.createAnonymous(commentWriterAccount);
//
//        FormAccount replyWriterAccount = accountSetup.createFormAccount();
//        Anonymous replyWriter = boardUserSetup.createAnonymous(replyWriterAccount);
//
//        Comment superComment = commentSetup.createComment(post,commentWriter);
//        Reply reply = replySetup.createReply(post,replyWriter,superComment,superComment);
//
//        String accessToken = tokenSetup.getAccessToken(replyWriterAccount);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                delete(url + "{replyId}",reply.getCommentId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .header("Authorization",accessToken)
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//        .andExpect(status().isOk());
//
//        em.flush(); em.clear();
//
//        Reply deleted = replyService.findById(reply.getCommentId());
//
//        assertEquals(deleted.getStatus(), Status.PERMANENT_DELETED);
//    }
//
//    //TODO : moreTest
//
}