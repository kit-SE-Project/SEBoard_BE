package com.seproject.board.service;

import org.junit.jupiter.api.TestInstance;

import static org.mockito.Mockito.mock;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BaseAppServiceTest {
//
//    protected CommentRepository commentRepository;
//    protected UnnamedCommentRepository unnamedCommentRepository;
//    protected AuthorRepository authorRepository;
//    protected PostRepository postRepository;
//    protected UnnamedPostRepository unnamedPostRepository;
//    protected CategoryRepository categoryRepository;
//    protected BookmarkRepository bookmarkRepository;
//
//    @BeforeAll
//    public void init() {
//        commentRepository = mock(CommentRepository.class);
//        authorRepository = mock(AuthorRepository.class);
//        postRepository = mock(PostRepository.class);
//        unnamedCommentRepository = mock(UnnamedCommentRepository.class);
//        unnamedPostRepository = mock(UnnamedPostRepository.class);
//        categoryRepository = mock(CategoryRepository.class);
//        bookmarkRepository = mock(BookmarkRepository.class);
//    }
//
//    protected Author createAuthor(Long authorId, String loginId, String name) {
//        Author author = Author.builder()
//                .authorId(authorId)
//                .loginId(loginId)
//                .name(name)
//                .build();
//
//        when(authorRepository.findById(authorId)).thenReturn(Optional.of(author));
//        return author;
//    }
//
//    protected Comment createMockComment(Long commentId, String contents, int year, int month, int day, Author author) {
//        Comment comment = Comment.builder()
//                .contents(contents)
//                .commentId(commentId)
//                .author(author)
//                .baseTime(new BaseTime(LocalDateTime.of(year, month, day, 0, 0), null))
//                .build();
//        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
//        return comment;
//    }
//
//    protected UnnamedComment createMockUnnamedComment(Long commentId, String contents, int year, int month, int day, Author author, String password) {
//        UnnamedComment comment = UnnamedComment.builder()
//                .commentId(commentId)
//                .contents(contents)
//                .author(author)
//                .baseTime(new BaseTime(LocalDateTime.of(year, month, day, 0, 0), null))
//                .password(password)
//                .build();
//        when(unnamedCommentRepository.findById(commentId)).thenReturn(Optional.of(comment));
//        return comment;
//    }
//
//    protected Comment createMockReply(Long commentId, Comment superComment, Comment taggedComment, Post post, int year, int month, int day, Author author) {
//        Comment reply = Comment.builder()
//                .commentId(commentId)
//                .superComment(superComment)
//                .tag(taggedComment)
//                .author(author)
//                .post(post)
//                .baseTime(new BaseTime(LocalDateTime.of(year, month, day, 0, 0), null))
//                .build();
//        when(commentRepository.findById(commentId)).thenReturn(Optional.of(reply));
//        return reply;
//    }
//
//    protected Post createMockPost(Long postId,Author author,Category category,String title,String contents,boolean pined){
//        Post post = Post.builder()
//                .postId(postId)
//                .author(author)
//                .category(category)
//                .title(title)
//                .contents(contents)
//                .pined(pined)
//                .views(0)
//                .baseTime(new BaseTime())
//                .build();
//
//        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
//        return post;
//    }
//
//    protected UnnamedPost createMockUnnamedPost(Long UnnamedPostId,Author author,Category category,String title, String contents,String password,boolean pined) {
//        UnnamedPost unnamedPost = UnnamedPost.builder()
//                .postId(UnnamedPostId)
//                .author(author)
//                .category(category)
//                .title(title)
//                .contents(contents)
//                .password(password)
//                .pined(pined)
//                .views(0)
//                .baseTime(new BaseTime())
//                .build();
//        when(unnamedPostRepository.findById(UnnamedPostId)).thenReturn(Optional.of(unnamedPost));
//        return unnamedPost;
//    }
//
//    protected Category createMockCategory(Long categoryId,Category superCategory,String name) {
//        Category category = Category.builder()
//                .categoryId(categoryId)
//                .superCategory(superCategory)
//                .name(name)
//                .build();
//
//        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
//        return category;
//    }
//
//    protected Bookmark createMockBookmark(Long bookmarkId,Long userId,Long postId) {
//
//        Bookmark bookmark = Bookmark.builder()
//                .bookmarkId(bookmarkId)
//                .userId(userId)
//                .postId(postId)
//                .build();
//
//        when(bookmarkRepository.existsByPostIdAndUserId(postId,userId)).thenReturn(true);
//        when(bookmarkRepository.findByPostIdAndUserId(postId,userId)).thenReturn(bookmark);
//
//        return bookmark;
//    }
}
