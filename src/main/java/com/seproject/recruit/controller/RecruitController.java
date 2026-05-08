package com.seproject.recruit.controller;

import com.seproject.recruit.application.RecruitAppService;
import com.seproject.recruit.controller.dto.RecruitPostRequest;
import com.seproject.recruit.controller.dto.RecruitPostResponse;
import com.seproject.recruit.domain.model.RecruitType;
import com.seproject.recruit.service.RecruitPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/recruit")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitAppService recruitAppService;

    @GetMapping
    public ResponseEntity<Page<RecruitPostResponse.RecruitPostListItem>> getList(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean includeClose,
            @RequestParam(defaultValue = "LATEST") String orderBy,
            @PageableDefault(size = 12) Pageable pageable) {
        RecruitType recruitType = type != null ? RecruitType.valueOf(type) : null;
        RecruitPostService.SortType sortType = RecruitPostService.SortType.valueOf(orderBy);
        return ResponseEntity.ok(recruitAppService.getList(recruitType, includeClose, sortType, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecruitPostResponse.RecruitPostDetail> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(recruitAppService.getDetail(id));
    }

    @PostMapping
    public ResponseEntity<RecruitPostResponse.RecruitPostDetail> create(
            @RequestBody RecruitPostRequest.Create request) {
        return ResponseEntity.ok(recruitAppService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecruitPostResponse.RecruitPostDetail> update(
            @PathVariable Long id, @RequestBody RecruitPostRequest.Update request) {
        return ResponseEntity.ok(recruitAppService.update(id, request));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Void> close(@PathVariable Long id) {
        recruitAppService.close(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recruitAppService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
