package com.seproject.board.post.domain.repository;

import com.seproject.board.common.Status;
import com.seproject.board.menu.domain.Category;
import com.seproject.board.post.domain.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;

import java.util.List;

public interface PostRepository extends JpaRepository<Post,Long> {
    @Query(value = "select exists(select * from posts where category_id = :categoryId)", nativeQuery = true)
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "select * from posts where category_id = :categoryId", nativeQuery = true)
    List<Post> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query(value = "select p from Post p join p.author join p.author.account where p.author.account.accountId = :accountId")
    List<Post> findByAccountId(@Param("accountId") Long accountId);

    @Modifying
    @Query("update Post p set p.reportCount = 0 , p.status = 'NORMAL' where p.postId in :postIds")
    void restorePostByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying
    @Query("update Post p set p.status = :status where p.postId in :postIds")
    void deleteAllByIds(@Param("postIds") List<Long> postIds , @Param("status")Status status);


    @Modifying
    @Query("update Post p set p.category = :to where p.category = :from")
    void changeCategory(@Param("from") Category from, @Param("to") Category to);

}
