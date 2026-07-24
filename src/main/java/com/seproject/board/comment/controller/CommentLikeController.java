package com.seproject.board.comment.controller;

import com.seproject.account.utils.SecurityUtils;
import com.seproject.board.comment.application.CommentLikeAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.post.domain.model.LikeType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/comments/{commentId}")
@Tag(name = "댓글 추천/비추천 API", description = "댓글 추천/비추천 관련 API")
public class CommentLikeController {

    private final CommentLikeAppService commentLikeAppService;

    @Parameter(name = "commentId", description = "추천할 댓글 pk")
    @Operation(summary = "댓글 추천", description = "댓글을 추천한다. 이미 추천한 경우 취소, 비추천 상태인 경우 추천으로 변경된다.")
    @PostMapping("/like")
    public ResponseEntity<?> like(@PathVariable Long commentId) {
        String loginId = SecurityUtils.getLoginId();
        LikeType result = commentLikeAppService.toggle(commentId, loginId, LikeType.LIKE);

        String message = (result == null) ? "추천 취소" : "추천 완료";
        return ResponseEntity.ok(MessageResponse.of(message));
    }

    @Parameter(name = "commentId", description = "비추천할 댓글 pk")
    @Operation(summary = "댓글 비추천", description = "댓글을 비추천한다. 이미 비추천한 경우 취소, 추천 상태인 경우 비추천으로 변경된다.")
    @PostMapping("/dislike")
    public ResponseEntity<?> dislike(@PathVariable Long commentId) {
        String loginId = SecurityUtils.getLoginId();
        LikeType result = commentLikeAppService.toggle(commentId, loginId, LikeType.DISLIKE);

        String message = (result == null) ? "비추천 취소" : "비추천 완료";
        return ResponseEntity.ok(MessageResponse.of(message));
    }
}