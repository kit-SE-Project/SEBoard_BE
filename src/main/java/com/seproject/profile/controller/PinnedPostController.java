package com.seproject.profile.controller;

import com.seproject.account.utils.SecurityUtils;
import com.seproject.profile.domain.PinnedPost;
import com.seproject.profile.service.PinnedPostService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile/pins")
@RequiredArgsConstructor
public class PinnedPostController {

    private final PinnedPostService pinnedPostService;

    @GetMapping("/{accountId}")
    public ResponseEntity<List<PinnedPostResponse>> getPins(@PathVariable Long accountId) {
        return ResponseEntity.ok(
                pinnedPostService.findByAccountId(accountId).stream()
                        .map(PinnedPostResponse::new)
                        .collect(Collectors.toList()));
    }

    @PostMapping
    public ResponseEntity<PinnedPostResponse> pin(@RequestBody PinRequest request) {
        Long accountId = getAccountId();
        return ResponseEntity.ok(new PinnedPostResponse(
                pinnedPostService.pin(accountId, request.getPostId())));
    }

    @DeleteMapping
    public ResponseEntity<Void> unpin(@RequestParam Long postId) {
        pinnedPostService.unpin(getAccountId(), postId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestBody ReorderRequest request) {
        pinnedPostService.reorder(getAccountId(), request.getPostIds());
        return ResponseEntity.noContent().build();
    }

    private Long getAccountId() {
        return SecurityUtils.getAccount()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."))
                .getAccountId();
    }

    @Data
    public static class PinRequest {
        private Long postId;
    }

    @Data
    public static class ReorderRequest {
        private List<Long> postIds;
    }

    @Data
    public static class PinnedPostResponse {
        private final Long id;
        private final Long postId;
        private final int orderIndex;

        public PinnedPostResponse(PinnedPost p) {
            this.id = p.getId();
            this.postId = p.getPostId();
            this.orderIndex = p.getOrderIndex();
        }
    }
}
