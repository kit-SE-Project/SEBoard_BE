package com.seproject.skill.controller;

import com.seproject.skill.domain.SkillCategory;
import com.seproject.skill.domain.SkillTag;
import com.seproject.skill.service.SkillTagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillTagController {

    private final SkillTagService skillTagService;

    @GetMapping
    public ResponseEntity<Page<SkillResponse>> getActiveSkills(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        SkillCategory cat = (category != null && !category.equals("ALL"))
                ? SkillCategory.valueOf(category) : null;

        Page<SkillTag> result = skillTagService.findActiveWithFilter(
                cat, keyword, PageRequest.of(page, size));

        return ResponseEntity.ok(result.map(SkillResponse::new));
    }

    @Data
    public static class SkillResponse {
        private final Long id;
        private final String name;
        private final String category;
        private final boolean isActive;
        private final String iconSlug;

        public SkillResponse(SkillTag tag) {
            this.id = tag.getId();
            this.name = tag.getName();
            this.category = tag.getCategory().name();
            this.isActive = tag.isActive();
            this.iconSlug = tag.getIconSlug();
        }
    }
}
