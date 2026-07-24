package com.seproject.recruit.domain.repository;

import com.seproject.recruit.domain.model.RecruitPost;
import com.seproject.recruit.domain.model.RecruitStatus;
import com.seproject.recruit.domain.model.RecruitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecruitPostRepository extends JpaRepository<RecruitPost, Long> {

    // 최신순
    Page<RecruitPost> findByStatusOrderByCreatedAtDesc(RecruitStatus status, Pageable pageable);
    Page<RecruitPost> findByTypeAndStatusOrderByCreatedAtDesc(RecruitType type, RecruitStatus status, Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.status != 'DELETED' ORDER BY r.createdAt DESC")
    Page<RecruitPost> findAllExcludeDeletedOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.type = :type AND r.status != 'DELETED' ORDER BY r.createdAt DESC")
    Page<RecruitPost> findByTypeExcludeDeletedOrderByCreatedAtDesc(@Param("type") RecruitType type, Pageable pageable);

    // 조회순
    Page<RecruitPost> findByStatusOrderByViewCountDesc(RecruitStatus status, Pageable pageable);
    Page<RecruitPost> findByTypeAndStatusOrderByViewCountDesc(RecruitType type, RecruitStatus status, Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.status != 'DELETED' ORDER BY r.viewCount DESC")
    Page<RecruitPost> findAllExcludeDeletedOrderByViewCountDesc(Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.type = :type AND r.status != 'DELETED' ORDER BY r.viewCount DESC")
    Page<RecruitPost> findByTypeExcludeDeletedOrderByViewCountDesc(@Param("type") RecruitType type, Pageable pageable);

    // 마감임박순 (end_date null은 뒤로)
    @Query("SELECT r FROM RecruitPost r WHERE r.status = :status ORDER BY CASE WHEN r.endDate IS NULL THEN 1 ELSE 0 END ASC, r.endDate ASC")
    Page<RecruitPost> findByStatusOrderByDeadline(@Param("status") RecruitStatus status, Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.type = :type AND r.status = :status ORDER BY CASE WHEN r.endDate IS NULL THEN 1 ELSE 0 END ASC, r.endDate ASC")
    Page<RecruitPost> findByTypeAndStatusOrderByDeadline(@Param("type") RecruitType type, @Param("status") RecruitStatus status, Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.status != 'DELETED' ORDER BY CASE WHEN r.endDate IS NULL THEN 1 ELSE 0 END ASC, r.endDate ASC")
    Page<RecruitPost> findAllExcludeDeletedOrderByDeadline(Pageable pageable);

    @Query("SELECT r FROM RecruitPost r WHERE r.type = :type AND r.status != 'DELETED' ORDER BY CASE WHEN r.endDate IS NULL THEN 1 ELSE 0 END ASC, r.endDate ASC")
    Page<RecruitPost> findByTypeExcludeDeletedOrderByDeadline(@Param("type") RecruitType type, Pageable pageable);

    @Query("SELECT r FROM RecruitPost r JOIN r.skills s WHERE s.id IN :skillIds AND r.status = 'ACTIVE' ORDER BY r.createdAt DESC")
    Page<RecruitPost> findBySkillsAndActive(List<Long> skillIds, Pageable pageable);

    List<RecruitPost> findTop6ByStatusOrderByCreatedAtDesc(RecruitStatus status);

    /** 구직 글의 스킬과 겹치는 활성 구인 글 작성자 accountId 목록 (자신 제외) */
    @Query("SELECT DISTINCT r.author.account.accountId FROM RecruitPost r " +
           "JOIN r.skills s " +
           "WHERE r.type = 'RECRUIT' AND r.status = 'ACTIVE' " +
           "AND s.skillTag.id IN :skillIds " +
           "AND r.author.account.accountId != :excludeAccountId")
    List<Long> findActiveRecruitAuthorAccountIdsBySkillIds(
            @Param("skillIds") List<Long> skillIds,
            @Param("excludeAccountId") Long excludeAccountId);

}
