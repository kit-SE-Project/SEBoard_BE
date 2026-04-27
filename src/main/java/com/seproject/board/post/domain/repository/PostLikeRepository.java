package com.seproject.board.post.domain.repository;

import com.seproject.board.post.domain.model.LikeType;
import com.seproject.board.post.domain.model.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("select pl from PostLike pl where pl.post.postId = :postId and pl.member.boardUserId = :memberId")
    Optional<PostLike> findByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId") Long memberId);

    @Query("select count(pl) from PostLike pl where pl.post.postId = :postId and pl.likeType = :likeType")
    int countByPostIdAndLikeType(@Param("postId") Long postId, @Param("likeType") LikeType likeType);
}