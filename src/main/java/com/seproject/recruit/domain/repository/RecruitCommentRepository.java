package com.seproject.recruit.domain.repository;

import com.seproject.recruit.domain.model.RecruitComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitCommentRepository extends JpaRepository<RecruitComment, Long> {

    // 삭제된 댓글도 포함 반환 — 프론트에서 isDeleted=true 이면 "삭제된 댓글입니다." 표시 (일반 Post와 동일)
    @Query("SELECT c FROM RecruitComment c WHERE c.recruitPost.id = :postId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<RecruitComment> findByRecruitPostIdAndParentCommentIsNullOrderByCreatedAtAsc(@Param("postId") Long postId);

    @Query("SELECT c FROM RecruitComment c WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    List<RecruitComment> findByParentCommentIdOrderByCreatedAtAsc(@Param("parentId") Long parentId);

    // 게시글 삭제 시 일괄 소프트 삭제 (일반 Post의 comments.forEach(comment -> comment.delete(true)) 와 동일)
    @Modifying
    @Query("UPDATE RecruitComment c SET c.isDeleted = true WHERE c.recruitPost.id = :postId")
    void softDeleteAllByPostId(@Param("postId") Long postId);
}
