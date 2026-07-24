package com.seproject.skill.service;

import com.seproject.skill.domain.SkillCategory;
import com.seproject.skill.domain.SkillTag;
import com.seproject.skill.repository.SkillTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SkillTagService {

    private final SkillTagRepository skillTagRepository;

    public List<SkillTag> findAllActive() {
        return skillTagRepository.findByIsActiveTrue();
    }

    public List<SkillTag> findAll() {
        return skillTagRepository.findAll();
    }

    public Page<SkillTag> findActiveWithFilter(SkillCategory category, String keyword, Pageable pageable) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return skillTagRepository.findActiveWithFilter(category, kw, pageable);
    }

    public Page<SkillTag> findAllWithFilter(SkillCategory category, String keyword, Pageable pageable) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim() : null;
        return skillTagRepository.findAllWithFilter(category, kw, pageable);
    }

    public List<SkillTag> findByIds(List<Long> ids) {
        return skillTagRepository.findAllById(ids);
    }

    @Transactional
    public SkillTag create(String name, SkillCategory category, String iconSlug) {
        return skillTagRepository.save(SkillTag.builder()
                .name(name).category(category).iconSlug(iconSlug).build());
    }

    @Transactional
    public SkillTag update(Long id, String name, SkillCategory category, String iconSlug) {
        SkillTag skill = skillTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("스킬을 찾을 수 없습니다."));
        skill.update(name, category, iconSlug);
        return skill;
    }

    @Transactional
    public void toggleActive(Long id) {
        SkillTag skill = skillTagRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("스킬을 찾을 수 없습니다."));
        skill.toggleActive();
    }
}
