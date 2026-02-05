package com.seproject.board.service;

import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommentAppServiceTest extends BaseAppServiceTest{
//
//    private CommentAppService commentAppService;
//    @BeforeAll
//    public void init() {
//        super.init();
//        commentAppService = new CommentAppService(unnamedCommentRepository,commentRepository,postRepository,authorRepository);
//        Author firstAuthor = createAuthor(1L, "hong", "홍길동");
//        Author secondAuthor = createAuthor(2L, "kim", "김민종");
//        Author thirdAuthor = createAuthor(3L, "lee", "이순신");
//
//        Post post = createMockPost(1L,null,null,null,null,true);
//
//        Comment first = createMockComment(1L,"comment1",3022,11,13,firstAuthor);
//            Comment firstReply = createMockReply(4L,first,first,post,2021,11,4,secondAuthor);
//            Comment secondReply = createMockReply(5L,first,firstReply,post,300,2,11,thirdAuthor);
//        Comment second = createMockComment(2L,"comment2",1021,11,13,secondAuthor);
//            Comment thirdReply = createMockReply(6L,second,second,post,1110,4,13,thirdAuthor);
//        Comment third = createMockComment(3L,"comment3",2000,11,13,firstAuthor);
//
//        List<Comment> stub = List.of(first, second, third, firstReply, secondReply, thirdReply);
//
//        when(commentRepository.findByPostId(1L)).thenReturn(new ArrayList<>(stub));
//    }
//    @Test
//    @DisplayName("게시글 상세 조회시 게시글에 달린 댓글들이 정상적으로 조회되는지 검사하는 기능")
//    void retrieveCommentListTest() {
//
//        List<CommentDTO.CommentListResponseDTO> commentListResponseDTOS = commentAppService.retrieveCommentList(1L, 1L);
//        List<Integer> ans = List.of(1,0,2);
//
//        for (int i = 0; i < commentListResponseDTOS.size(); i++) {
//            CommentDTO.CommentListResponseDTO commentListResponseDTO = commentListResponseDTOS.get(i);
//            System.out.println(commentListResponseDTO);
//            commentListResponseDTO.getReplies().forEach(reply -> {
//                System.out.println("---> " + reply);
//            });
//            System.out.println("---------------------------");
//        }
//
//        for (int i = 0; i < 3; i++) {
//            CommentDTO.CommentListResponseDTO commentListResponseDTO = commentListResponseDTOS.get(i);
//            Assertions.assertEquals(commentListResponseDTO.getReplies().size(),ans.get(i));
//        }
//
//    }
//
//    @Test
//    @DisplayName("답글 작성 테스트")
//    void writeNamedReplyTests() {
//
//        Long postId = 2L;
//        Post post = createMockPost(postId,null,null,null,null,false);
//
//        Long superCommentId = 2L;
//        Comment superComment = createMockComment(superCommentId,"superComment",2012,10,10,null);
//
//        Long taggedCommentId = 3L;
//        Comment taggedComment = createMockComment(superCommentId,"taggedComment",2013,11,10,null);
//
//        Long userId = 4L;
//        Author author = createAuthor(userId,null,null);
//
//        commentAppService.createNamedReply(userId,postId,superCommentId,taggedCommentId,"댓글 내용");
//    }
//
//    @Test
//    @DisplayName("답글 작성 실패 - 사용자 정보가 없는경우 테스트")
//    void writeNamedReplyNoAuthorTests() {
//
//        Long postId = 2L;
//        Post post = createMockPost(postId,null,null,null,null,false);
//
//        Long superCommentId = 2L;
//        Comment superComment = createMockComment(superCommentId,"superComment",2012,10,10,null);
//
//        Long taggedCommentId = 3L;
//        Comment taggedComment = createMockComment(superCommentId,"taggedComment",2013,11,10,null);
//
//        Long userId = 123414L;
//
//        Assertions.assertThrows(NoSuchElementException.class, () -> {
//            commentAppService.createNamedReply(userId,postId,superCommentId,taggedCommentId,"댓글 내용");
//        });
//    }
//
//    @Test
//    @DisplayName("답글 수정 테스트")
//    void updateNamedReplyTests() {
//
//        Long userId = 4L;
//        Author author = createAuthor(userId,null,null);
//
//        Long postId = 2L;
//        Post post = createMockPost(postId,author,null,null,null,false);
//
//        Long superCommentId = 2L;
//        Comment superComment = createMockComment(superCommentId,"superComment",2012,10,10,null);
//
//        Long replyId = 3L;
//        String newContents = "new Contents";
//
//        Comment reply = createMockReply(replyId,superComment,superComment,post,2013,11,10,author);
//
//        Assertions.assertTrue(reply.isWrittenBy(author));
//        Assertions.assertTrue(reply.isNamed());
//
//        commentAppService.changeContentsOfNamed(replyId,userId,newContents);
//        String contents = reply.getContents();
//
//        Assertions.assertEquals(contents,newContents);
//    }
//
//    @Test
//    @DisplayName("답글 수정 테스트 - 익명 답글이 선택된 경우")
//    void updateNamedReplyWhenUnnamedSelectedTests () {
//        Long userId = 4L;
//        Long replyId = 4L;
//        String newContents = "new Contents";
//
//        Author author = createAuthor(userId, null, null);
//
//        UnnamedComment reply = createMockUnnamedComment(replyId, "it is reply", 2012, 1, 13, null, "originPassword");
//
//        when(commentRepository.findById(replyId)).thenReturn(Optional.of(reply));
//
//        Assertions.assertFalse(reply.isNamed());
//        Assertions.assertThrows(IllegalArgumentException.class, () -> commentAppService.changeContentsOfNamed(replyId, userId, newContents));
//        String contents = reply.getContents();
//        Assertions.assertNotEquals(contents, newContents);
//    }
//
//
//    @Test
//    @DisplayName("익명 작성자의 댓글 내용을 수정하는 테스트")
//    void changeContentsOfUnnamedTest() {
//        Long userId = 4L;
//        Author author = createAuthor(userId,"loginId","Halland");
//        Long unnamedCommentId = 10L;
//        String password = "password";
//        UnnamedComment unnamedComment = createMockUnnamedComment(unnamedCommentId,"origin contents",2012,12,25,author,password);
//
//        String newContents = "new contents";
//        commentAppService.changeContentsOfUnnamed(unnamedCommentId,password,newContents);
//        Assertions.assertEquals(unnamedComment.getContents(),newContents);
//    }
//
//    @Test
//    @DisplayName("익명 작성자의 댓글 내용을 수정이 비밀번호가 틀려 실패하는 테스트")
//    void changeContentsOfUnnamedBadCredentialTest() {
//        Long userId = 4L;
//        Author author = createAuthor(userId,"loginId","Halland");
//        Long unnamedCommentId = 10L;
//        String password = "password";
//        String originContents = "origin contents";
//        UnnamedComment unnamedComment = createMockUnnamedComment(unnamedCommentId,originContents,2012,12,25,author,password);
//
//        String newContents = "new contents";
//        Assertions.assertThrows(IllegalArgumentException.class,() -> {
//            commentAppService.changeContentsOfUnnamed(unnamedCommentId,"incorrect password",newContents);
//        });
//
//        Assertions.assertEquals(unnamedComment.getContents(),originContents);
//    }
//
//    @Test
//    @DisplayName("익명댓글 삭제 성공")
//    void deleteUnnamedCommentTest() {
//        Long userId = 4L;
//        Author author = createAuthor(userId,"loginId","Halland");
//        Long unnamedCommentId = 10L;
//        String password = "password";
//        String originContents = "origin contents";
//        UnnamedComment unnamedComment = createMockUnnamedComment(unnamedCommentId,originContents,2012,12,25,author,password);
//
//        Assertions.assertDoesNotThrow(() -> {
//            commentAppService.deleteUnnamedComment(unnamedCommentId,password);
//        });
//
//    }
//
//    @Test
//    @DisplayName("비밀번호가 틀려서 익명 댓글 삭제 실패")
//    void deleteUnnamedCommentBadCredentialTest() {
//        Long userId = 4L;
//        Author author = createAuthor(userId,"loginId","Halland");
//        Long unnamedCommentId = 10L;
//        String password = "password";
//        String originContents = "origin contents";
//        UnnamedComment unnamedComment = createMockUnnamedComment(unnamedCommentId,originContents,2012,12,25,author,password);
//
//        Assertions.assertThrows(IllegalArgumentException.class,() -> {
//            commentAppService.deleteUnnamedComment(unnamedCommentId,"incorrect password");
//        });
//
//    }
}
