package com.seproject.board.common.controller;

import com.seproject.global.IntegrationTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ProfileControllerTest extends IntegrationTestSupport {
//
//    static final String url = "/profile/";
//
//    @Test
//    public void 로그인_프로필_정보_조회() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//
//        commentSetup.createComment(post,boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//        commentSetup.createComment(post,boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//
//        Bookmark bookmark = bookmarkSetup.createBookmark(post, member);
//
//        em.flush(); em.clear();
//        mvc.perform(
//                get(url + "{loginId}", formAccount.getLoginId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//                        .header("Authorization",accessToken)
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.nickname").value(member.getName()))
//                .andExpect(jsonPath("$.postCount").value(1))
//                .andExpect(jsonPath("$.commentCount").value(1))
//                .andExpect(jsonPath("$.bookmarkCount").value(1));
//    }
//    @Test
//    public void 비로그인_프로필_정보_조회() throws Exception {
//
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//        Comment comment = commentSetup.createComment(post, member);
//
//        commentSetup.createComment(post,boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//        commentSetup.createComment(post,boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//
//
//        mvc.perform(
//                        get(url + "{loginId}", formAccount.getLoginId())
//                                .accept(MediaType.APPLICATION_JSON)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.nickname").value(member.getName()))
//                .andExpect(jsonPath("$.postCount").value(1))
//                .andExpect(jsonPath("$.commentCount").value(1))
//                .andExpect(jsonPath("$.bookmarkCount").isEmpty());
//    }
//    @Test
//    public void 비로그인_유저가_작성한_게시글_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        List<Post> posts = new ArrayList<>();
//        for (int i = 0; i < 3; i++) {
//            Post post = postSetup.createPost(member, category);
//            posts.add(post);
//        }
//
//        for (int i = 0; i < 3; i++) {
//            Post post = postSetup.createPost(boardUserSetup.createAnonymous(formAccount), category);
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                get(url + "{loginId}/posts",formAccount.getLoginId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .accept(MediaType.APPLICATION_JSON)
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(3));
////                .andExpect(jsonPath("$.content[0].postId").value(posts.get(0).getPostId()))
////                .andExpect(jsonPath("$.content[1].postId").value(posts.get(1).getPostId()))
////                .andExpect(jsonPath("$.content[2].postId").value(posts.get(2).getPostId()));
//
//    }
//    @Test
//    public void 로그인_유저가_작성한_게시글_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        List<Post> posts = new ArrayList<>();
//        for (int i = 0; i < 3; i++) {
//            Post post = postSetup.createPost(member, category);
//            posts.add(post);
//        }
//
//        for (int i = 0; i < 3; i++) {
//            Post post = postSetup.createPost(boardUserSetup.createAnonymous(formAccount), category);
//        }
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//        em.flush(); em.clear();
//
//        mvc.perform(
//                        get(url + "{loginId}/posts",formAccount.getLoginId())
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .accept(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                                .header("Authorization",accessToken)
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(6));
//    }
//    @Test
//    public void 북마크_목록_조회() throws Exception {
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//
//        Post post = postSetup.createPost(member, category);
//        Post post1 = postSetup.createPost(boardUserSetup.createAnonymous(accountSetup.createFormAccount()), category);
//
//        Bookmark bookmark = bookmarkSetup.createBookmark(post, member);
//        Bookmark bookmark2 = bookmarkSetup.createBookmark(post1, member);
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                get(url + "{loginId}/bookmarks",formAccount.getLoginId())
//                        .accept(MediaType.APPLICATION_JSON)
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .header("Authorization",tokenSetup.getAccessToken(formAccount))
//                        .characterEncoding("UTF-8")
//        ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(2));
//    }
//    @Test
//    public void 비로그인_북마크_목록_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        mvc.perform(
//                        get(url + "{loginId}/bookmarks",formAccount.getLoginId())
//                                .accept(MediaType.APPLICATION_JSON)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isUnauthorized());
//    }
//
//    @Test
//    public void 로그인_사용자가_작성한_댓글_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        List<Comment> list = new ArrayList<>();
//
//        for (int i = 0; i < 3; i++) {
//            Comment comment = commentSetup.createComment(post, boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//            list.add(comment);
//        }
//
//        for (int i = 0; i < 2; i++) {
//            replySetup.createReply(post,boardUserSetup.createAnonymous(formAccount),list.get(i),list.get(i));
//        }
//
//        String accessToken = tokenSetup.getAccessToken(formAccount);
//        em.flush(); em.clear();
//
//        mvc.perform(
//                        get(url + "{loginId}/comments",formAccount.getLoginId())
//                                .accept(MediaType.APPLICATION_JSON)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                                .header("Authorization",accessToken)
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(2));
//
//    }
//
//    @Test
//    public void 비로그인_사용자가_작성한_댓글_조회() throws Exception {
//        FormAccount formAccount = accountSetup.createFormAccount();
//        Member member = boardUserSetup.createMember(formAccount);
//        Category category = menuSetup.createCategory(menuSetup.createBoardMenu(menuSetup.createMenu()));
//
//        Post post = postSetup.createPost(member, category);
//
//        List<Comment> list = new ArrayList<>();
//
//        for (int i = 0; i < 3; i++) {
//            Comment comment = commentSetup.createComment(post, boardUserSetup.createAnonymous(accountSetup.createFormAccount()));
//            list.add(comment);
//        }
//
//        for (int i = 0; i < 2; i++) {
//            replySetup.createReply(post,boardUserSetup.createAnonymous(formAccount),list.get(i),list.get(i));
//        }
//
//        em.flush(); em.clear();
//
//        mvc.perform(
//                        get(url + "{loginId}/comments",formAccount.getLoginId())
//                                .accept(MediaType.APPLICATION_JSON)
//                                .contentType(MediaType.APPLICATION_JSON)
//                                .characterEncoding("UTF-8")
//                ).andDo(print())
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.content.size()").value(0));
//
//    }
//
//
//
//
//
//
}