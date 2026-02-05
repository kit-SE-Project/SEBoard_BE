package com.seproject.board.post.controller;

import com.seproject.account.utils.SecurityUtils;
import com.seproject.board.post.application.BookmarkAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RestController
@RequestMapping("/posts/{postId}/bookmark")
@Tag(name = "북마크 API", description = "게시물 즐겨찾기 관련 API")
public class BookmarkController {
    private final BookmarkAppService bookmarkAppService;

    @Parameter(name = "postId", description = "즐겨찾기 지정할 게시물의 pk")
    @Operation(summary = "게시글 북마크 지정", description = "사용자가 게시글을 즐겨찾기로 등록한다")
    @PostMapping
    public ResponseEntity<?> createBookmark(@PathVariable Long postId){
        String loginId = SecurityUtils.getLoginId();

        bookmarkAppService.enrollBookmark(postId, loginId);

        return ResponseEntity.ok().body(MessageResponse.of("즐겨찾기 등록 완료"));
    }

    @Parameter(name = "postId", description = "즐겨찾기 해제할 게시물의 pk")
    @Operation(summary = "게시글 북마크 해제", description = "사용자가 즐겨찾기한 게시물을 즐겨찾기 해제한다")
    @DeleteMapping
    public ResponseEntity<?> cancelBookmark(@PathVariable Long postId){
        String loginId = SecurityUtils.getLoginId();

        bookmarkAppService.cancelBookmark(postId, loginId);

        return ResponseEntity.ok().body(MessageResponse.of("즐겨찾기 해제 완료"));
    }
}
