package com.seproject.board.service;

//import static org.mockito.Mockito.mock;
//
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostAppServiceTest extends BaseAppServiceTest {
//
//    private PostAppService postAppService;
//    private PostService postService;
//    @Override
//    @BeforeAll
//    public void init() {
//        super.init();
//        postService = mock(PostService.class);
//        postAppService = new PostAppService(postService,postRepository,unnamedPostRepository,authorRepository,categoryRepository,commentRepository,bookmarkRepository);
//    }
//
//    @Test
//    @DisplayName("게시글 삭제 성공 케이스")
//    void deleteNamedPostTest() {
//        Author author = createAuthor(1L,"alswhd1113","김민종");
//        Post post = createMockPost(1L,author,null,"title","contents",false);
//        Assertions.assertDoesNotThrow(() -> postAppService.deleteNamedPost(1L,1L));
//    }
//
//    @Test
//    @DisplayName("본인의 글이 아니면 게시글 삭제 실패")
//    void deleteNamedPostNotWriterTest() {
//        Author author = createAuthor(1L,"alswhd1113","김민종");
//        Author requestUser = createAuthor(2L,"loginId","김민종");
//        Post post = createMockPost(1L,author,null,"title","contents",false);
//        Assertions.assertThrows(IllegalArgumentException.class,() -> postAppService.deleteNamedPost(1L,2L));
//    }
//
//    @Test
//    @DisplayName("게시글을 조회했는데 자신의 글이라서 수정 권한이 존재함")
//    void retrieveNamedPostIsWriterTest() {
//        Author author = createAuthor(1L,"alswhd1113","김민종");
//        Category category = createMockCategory(1L,null,"free");
//        Post post = createMockPost(1L,author,category,"title","contents",false);
//        PostDTO.PostResponseDTO postResponseDTO = postAppService.retrieveNamedPost(1L, 1L);
//        Assertions.assertTrue(postResponseDTO.isEditable());
//        Assertions.assertFalse(postResponseDTO.isBookmarked());
//        Assertions.assertEquals(post.getPostId(),postResponseDTO.getPostId());
//    }
//
//    @Test
//    @DisplayName("게시글을 조회했는데 자신의 글이 아니면 수정 권한이 없음, 북마크한 글일때 북마크 여부가 포함되야함")
//    void retrieveBookmarkedNamedPostIsNotWriterTest() {
//        Author author = createAuthor(1L,"alswhd1113","김민종");
//        Author requestUser = createAuthor(2L,"laon198","김진우");
//        Category category = createMockCategory(1L,null,"free");
//        Post post = createMockPost(1L,author,category,"title","contents",false);
//        Bookmark bookmark = createMockBookmark(1L,2L,1L);
//        PostDTO.PostResponseDTO postResponseDTO = postAppService.retrieveNamedPost(1L, 2L);
//        Assertions.assertFalse(postResponseDTO.isEditable());
//        Assertions.assertTrue(postResponseDTO.isBookmarked());
//        Assertions.assertEquals(post.getPostId(),postResponseDTO.getPostId());
//    }
}
