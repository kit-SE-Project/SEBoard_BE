package com.seproject.board.common.domain.repository;

import com.seproject.board.common.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("select r from Report r where r.targetId = :postId and r.memberId = :memberId and r.reportType = 'POST'")
    Optional<Report> findByPostIdAndMemberId(@Param("postId") Long postId,@Param("memberId") Long memberId);

    @Query("select r from Report r where r.targetId = :commentId and r.memberId = :memberId and r.reportType = 'COMMENT'")
    Optional<Report> findByCommentIdAndMemberId(@Param("commentId") Long commentId,@Param("memberId") Long memberId);

    @Modifying
    @Query("delete Report r where r.targetId = :postId and r.reportType = 'POST'")
    void deleteAllByPostId(@Param("postId") Long postId);

    @Modifying
    @Query("delete Report r where r.targetId in :postIds and r.reportType = 'POST'")
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);

    @Modifying //N+1
    @Query("delete Report r where r.targetId = :commentId and r.reportType = 'COMMENT'")
    void deleteAllByCommentId(@Param("commentId") Long commentId);

    @Modifying
    @Query("delete Report r where r.targetId in :commentIds and r.reportType = 'COMMENT'")
    void deleteAllByCommentId(@Param("commentIds") List<Long> commentIds);
}
