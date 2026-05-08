package com.seproject.admin.recruit.controller;

import com.seproject.recruit.domain.model.RecruitMainPageConfig;
import com.seproject.recruit.domain.model.RecruitPost;
import com.seproject.recruit.domain.model.RecruitStatus;
import com.seproject.recruit.domain.model.RecruitType;
import com.seproject.recruit.service.RecruitMainPageConfigService;
import com.seproject.recruit.service.RecruitPostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/recruit")
@RequiredArgsConstructor
public class AdminRecruitController {

    private final RecruitPostService recruitPostService;
    private final RecruitMainPageConfigService configService;

    @GetMapping
    public ResponseEntity<Page<PostSummary>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        RecruitType recruitType = type != null ? RecruitType.valueOf(type) : null;
        boolean includeClose = status == null || !status.equalsIgnoreCase("ACTIVE");
        return ResponseEntity.ok(recruitPostService.findAll(recruitType, includeClose, RecruitPostService.SortType.LATEST, pageable)
                .map(PostSummary::new));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> forceClose(@PathVariable Long id) {
        recruitPostService.forceClose(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> forceDelete(@PathVariable Long id) {
        recruitPostService.forceDelete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/main-page-config")
    public ResponseEntity<ConfigResponse> getConfig() {
        return ResponseEntity.ok(new ConfigResponse(configService.getConfig()));
    }

    @PutMapping("/main-page-config")
    public ResponseEntity<ConfigResponse> updateConfig(@RequestBody ConfigRequest request) {
        RecruitMainPageConfig config = configService.update(
                request.isVisible(),
                request.getDisplayCount(),
                RecruitMainPageConfig.SortType.valueOf(request.getSortType()));
        return ResponseEntity.ok(new ConfigResponse(config));
    }

    @Data
    public static class PostSummary {
        private final Long id;
        private final String type;
        private final String tag;
        private final String title;
        private final String status;
        private final String authorName;

        public PostSummary(RecruitPost p) {
            this.id = p.getId();
            this.type = p.getType().name();
            this.tag = p.getTag().name();
            this.title = p.getTitle();
            this.status = p.getStatus().name();
            this.authorName = p.getAuthor() != null ? p.getAuthor().getName() : null;
        }
    }

    @Data
    public static class ConfigRequest {
        private boolean visible;
        private int displayCount;
        private String sortType;
    }

    @Data
    public static class ConfigResponse {
        private final boolean visible;
        private final int displayCount;
        private final String sortType;

        public ConfigResponse(RecruitMainPageConfig c) {
            this.visible = c.isVisible();
            this.displayCount = c.getDisplayCount();
            this.sortType = c.getSortType().name();
        }
    }
}
