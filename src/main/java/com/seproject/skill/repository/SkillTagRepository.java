package com.seproject.skill.repository;

import com.seproject.skill.domain.SkillCategory;
import com.seproject.skill.domain.SkillTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SkillTagRepository extends JpaRepository<SkillTag, Long> {

    List<SkillTag> findByIsActiveTrue();

    boolean existsByName(String name);

    @Query("SELECT s FROM SkillTag s WHERE s.isActive = true " +
           "AND (:category IS NULL OR s.category = :category) " +
           "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SkillTag> findActiveWithFilter(
            @Param("category") SkillCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT s FROM SkillTag s " +
           "WHERE (:category IS NULL OR s.category = :category) " +
           "AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SkillTag> findAllWithFilter(
            @Param("category") SkillCategory category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
