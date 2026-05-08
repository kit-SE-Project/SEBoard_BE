package com.seproject.admin.skill.controller;

import com.seproject.skill.controller.SkillTagController;
import com.seproject.skill.domain.SkillCategory;
import com.seproject.skill.service.SkillTagService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/skills")
@RequiredArgsConstructor
public class AdminSkillController {

    private final SkillTagService skillTagService;

    @GetMapping
    public ResponseEntity<Page<SkillTagController.SkillResponse>> getAll(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SkillCategory cat = (category != null && !category.isBlank()) ? SkillCategory.valueOf(category) : null;
        Page<SkillTagController.SkillResponse> result = skillTagService
                .findAllWithFilter(cat, keyword, PageRequest.of(page, size))
                .map(SkillTagController.SkillResponse::new);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<SkillTagController.SkillResponse> create(@RequestBody SkillRequest request) {
        return ResponseEntity.ok(new SkillTagController.SkillResponse(
                skillTagService.create(request.getName(), SkillCategory.valueOf(request.getCategory()),
                        request.getIconSlug())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillTagController.SkillResponse> update(
            @PathVariable Long id, @RequestBody SkillRequest request) {
        return ResponseEntity.ok(new SkillTagController.SkillResponse(
                skillTagService.update(id, request.getName(), SkillCategory.valueOf(request.getCategory()),
                        request.getIconSlug())));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleActive(@PathVariable Long id) {
        skillTagService.toggleActive(id);
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class SkillRequest {
        private String name;
        private String category;
        private String iconSlug;
    }
}
