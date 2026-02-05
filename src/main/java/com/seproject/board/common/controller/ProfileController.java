package com.seproject.board.common.controller;

import com.seproject.board.common.application.ProfileAppService;
import com.seproject.board.comment.controller.dto.CommentResponse.RetrieveCommentProfileElement;
import com.seproject.board.post.controller.dto.PostResponse.RetrievePostListResponseElement;
import com.seproject.board.common.controller.dto.ProfileResponse.ProfileInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileAppService profileAppService;

    @Operation(summary = "프로필 정보 조회")
    @GetMapping("/{memberId}")
    public ResponseEntity<ProfileInfoResponse> retrieveProfileInfo(@PathVariable Long memberId){
        ProfileInfoResponse profileInfoResponse = profileAppService.retrieveProfileInfo(memberId);
        return ResponseEntity.ok().body(profileInfoResponse);
    }

    @Operation(summary = "사용자가 작성한 게시글 조회")
    @GetMapping("/{memberId}/posts")
    public ResponseEntity<Page<RetrievePostListResponseElement>> retrieveMyPosts(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int perPage) {

        Page<RetrievePostListResponseElement> res = profileAppService.retrieveMyPost(memberId, page, perPage);
        return ResponseEntity.ok().body(res);
    }

    @Operation(summary = "내 북마크 목록 조회")
    @GetMapping("/{memberId}/bookmarks")
    public ResponseEntity<Page<RetrievePostListResponseElement>> retrieveBookmarkPost(
            @PathVariable Long memberId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int perPage){

        Page<RetrievePostListResponseElement> res = profileAppService.retrieveBookmarkPost(memberId, page, perPage);
        return ResponseEntity.ok().body(res);
    }

    @Operation(summary = "사용자가 작성한 댓글 목록 조회")
    @GetMapping("/{memberId}/comments")
    public ResponseEntity<Page<RetrieveCommentProfileElement>> retrieveMyComments(
            @PathVariable Long memberId,
            @RequestParam(defaultValue ="0") int page,
            @RequestParam(defaultValue = "10") int perPage) {

        Page<RetrieveCommentProfileElement> res = profileAppService.retrieveMyComment(memberId, page, perPage);
        return ResponseEntity.ok().body(res);
    }
}
