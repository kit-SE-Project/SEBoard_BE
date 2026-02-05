package com.seproject.board.common.domain.repository;

import com.seproject.board.common.domain.ReportThreshold;
import com.seproject.board.common.domain.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ReportThresholdRepository extends JpaRepository<ReportThreshold, Long> {
    @Query("select rt from ReportThreshold rt where rt.thresholdType = 'POST'")
    Optional<ReportThreshold> findPostThreshold();
    @Query("select rt from ReportThreshold rt where rt.thresholdType = 'COMMENT'")
    Optional<ReportThreshold> findCommentThreshold();
    boolean existsByThresholdType(ReportType type);

}
