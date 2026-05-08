package com.seproject.recruit.controller;

import com.seproject.account.utils.SecurityUtils;
import com.seproject.file.domain.model.AttachableType;
import com.seproject.file.domain.repository.FileMetaDataRepository;
import com.seproject.member.domain.Member;
import com.seproject.recruit.domain.model.RecruitComment;
import com.seproject.recruit.service.RecruitCommentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/recruit/{recruitPostId}/comments")
@RequiredArgsConstructor
public class RecruitCommentController {

    private final RecruitCommentService recruitCommentService;
    private final FileMetaDataRepository fileMetaDataRepository;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long recruitPostId) {
        List<RecruitComment> comments = recruitCommentService.findComments(recruitPostId);
        List<CommentResponse> result = comments.stream()
                .map(c -> {
                    List<RecruitComment> replies = recruitCommentService.findReplies(c.getId());
                    return new CommentResponse(c, replies, fileMetaDataRepository);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long recruitPostId,
            @RequestBody CommentRequest request) {
        Long accountId = getAccountId();
        RecruitComment comment = recruitCommentService.create(
                recruitPostId, accountId, request.getContents(),
                request.getParentCommentId(), request.isReadOnlyAuthor());
        return ResponseEntity.ok(new CommentResponse(comment, List.of(), fileMetaDataRepository));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommentResponse> update(
            @PathVariable Long recruitPostId,
            @PathVariable Long id,
            @RequestBody CommentRequest request) {
        Long accountId = getAccountId();
        RecruitComment comment = recruitCommentService.update(
                id, accountId, request.getContents(), request.isReadOnlyAuthor());
        return ResponseEntity.ok(new CommentResponse(comment, List.of(), fileMetaDataRepository));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long recruitPostId,
            @PathVariable Long id) {
        recruitCommentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Long getAccountId() {
        return SecurityUtils.getAccount()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."))
                .getAccountId();
    }

    @Data
    public static class CommentRequest {
        private String contents;
        private Long parentCommentId;
        private boolean readOnlyAuthor = false;
    }

    @Data
    public static class AuthorInfo {
        private final Long userId;   // BoardUser.boardUserId — UserResponse와 동일 기준
        private final String name;
        private final String profileImageUrl;
        private final String frameGradientStart;
        private final String frameGradientEnd;
        private final String badgeType;
        private final String badgeLabel;

        public AuthorInfo(Member member, FileMetaDataRepository fileRepo) {
            this.userId = member.getBoardUserId();
            this.name = member.getName();
            this.profileImageUrl = fileRepo
                    .findByAttachableTypeAndAttachableId(AttachableType.PROFILE, member.getBoardUserId())
                    .stream().findFirst().map(f -> f.getUrlPath()).orElse(null);

            if (member.getEquippedFrame() != null) {
                this.frameGradientStart = member.getEquippedFrame().getGradientStart();
                this.frameGradientEnd = member.getEquippedFrame().getGradientEnd();
            } else {
                this.frameGradientStart = null;
                this.frameGradientEnd = null;
            }

            this.badgeType = member.getAccount().getRoles().stream()
                    .filter(r -> r.getBadgeType() != null && r.getBadgePriority() != null)
                    .min(java.util.Comparator.comparingInt(r -> r.getBadgePriority()))
                    .map(r -> r.getBadgeType()).orElse(null);
            this.badgeLabel = member.getAccount().getRoles().stream()
                    .filter(r -> r.getBadgeType() != null && r.getBadgePriority() != null)
                    .min(java.util.Comparator.comparingInt(r -> r.getBadgePriority()))
                    .map(r -> r.toString()).orElse(null);
        }
    }

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Data
    public static class CommentResponse {
        private final Long id;
        private final AuthorInfo author;
        private final String contents;
        private final boolean readOnlyAuthor;
        private final boolean deleted;
        private final Long parentCommentId;
        private final Long tagCommentId;
        private final String createdAt;
        private final String modifiedAt;
        private final List<CommentResponse> replies;

        public CommentResponse(RecruitComment c, List<RecruitComment> replies,
                                FileMetaDataRepository fileRepo) {
            this.id = c.getId();
            this.deleted = c.isDeleted();
            this.author = c.isDeleted() ? null : (c.getAuthor() instanceof Member)
                    ? new AuthorInfo((Member) c.getAuthor(), fileRepo)
                    : null;
            this.contents = c.isDeleted() ? null : c.getContents();
            this.readOnlyAuthor = c.isReadOnlyAuthor();
            this.parentCommentId = c.getParentComment() != null ? c.getParentComment().getId() : null;
            this.tagCommentId = c.getTagCommentId();
            this.createdAt = c.getCreatedAt() != null ? c.getCreatedAt().format(DT_FMT) : null;
            this.modifiedAt = c.getModifiedAt() != null ? c.getModifiedAt().format(DT_FMT) : null;
            this.replies = replies.stream()
                    .map(r -> new CommentResponse(r, List.of(), fileRepo))
                    .collect(Collectors.toList());
        }
    }
}
