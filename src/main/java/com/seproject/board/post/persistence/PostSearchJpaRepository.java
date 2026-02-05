package com.seproject.board.post.persistence;

import com.seproject.board.post.controller.dto.PostResponse;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.post.domain.repository.PostSearchRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostSearchJpaRepository extends PostSearchRepository {
    @Query("select count(p) from Post p right join Member m on p.author.boardUserId=m.boardUserId where p.author.account.loginId = :loginId and p.status = 'NORMAL'")
    Integer countsMemberPostByLoginId(@Param("loginId") String loginId);

    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p join Bookmark b on p.postId=b.markedPost.postId where b.member.account.loginId=:loginId and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p join Bookmark b on p.postId=b.markedPost.postId where b.member.account.loginId=:loginId and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findBookmarkPostByLoginId(@Param("loginId") String loginId, Pageable pagingInfo);

    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p right join Member m on p.author.boardUserId=m.boardUserId where p.author.account.loginId = :loginId  and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p right join Member m on p.author.boardUserId=m.boardUserId where p.author.account.loginId = :loginId and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findMemberPostByLoginId(@Param("loginId") String loginId, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where p.author.account.loginId = :loginId and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where p.author.account.loginId = :loginId and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findPostByLoginId(@Param("loginId") String loginId, Pageable pagingInfo);
    @Query("select count(*) from Post p where p.author.account.loginId = :loginId and p.status = 'NORMAL'")
    Integer countsPostByLoginId(@Param("loginId") String loginId);
    @Query("select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostDetailResponse(p) " +
            "from Post p where p.postId = :id and p.status = 'NORMAL'")
    Optional<PostResponse.RetrievePostDetailResponse> findPostDetailById(@Param("id") Long id);
    @Query("select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where (p.category.menuId = :categoryId or p.category.superMenu.menuId = :categoryId) and p.pined = true and p.status = 'NORMAL' order by p.baseTime.createdAt desc")
    List<RetrievePostListResponseElement> findPinedPostByCategoryId(@Param("categoryId") Long categoryId);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where (p.category.menuId = :categoryId or p.category.superMenu.menuId = :categoryId) and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
            countQuery = "select count(p) from Post p where (p.category.menuId = :categoryId or p.category.superMenu.menuId = :categoryId) and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findPostByCategoryId(@Param("categoryId") Long categoryId, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where p.title like %:title% and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where p.title like %:title% and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findByTitle(@Param("title") String title, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where p.contents like %:content% and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where p.contents like %:content% and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findByContents(@Param("content") String content, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where (p.title like %:query% or p.contents like %:query%) and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where (p.title like %:query% or p.contents like %:query%) and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findByTitleOrContents(@Param("query") String query, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where p.author.name like %:authorName% and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where p.author.name like %:authorName% and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findByAuthorName(@Param("authorName") String authorName, Pageable pagingInfo);
    @Query(value = "select new com.seproject.board.post.controller.dto.PostResponse$RetrievePostListResponseElement(p)" +
            "from Post p where (p.title like %:searchQuery% or p.contents like %:searchQuery% or p.author.name like %:searchQuery%) and p.status = 'NORMAL' order by p.baseTime.createdAt desc",
    countQuery = "select count(p) from Post p where (p.title like %:searchQuery% or p.contents like %:searchQuery% or p.author.name like %:searchQuery%) and p.status = 'NORMAL'")
    Page<RetrievePostListResponseElement> findByAllOptions(@Param("searchQuery") String searchQuery, Pageable pagingInfo);
}
