package com.seproject.admin.comment.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seproject.admin.comment.application.AdminCommentAppService;
import com.seproject.admin.comment.controller.condition.AdminCommentRetrieveCondition;
import com.seproject.board.common.controller.dto.MessageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.admin.comment.controller.dto.CommentDTO.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/comments")
public class AdminCommentController {
    private final AdminCommentAppService adminCommentAppService;

    @Operation(summary = "댓글 목록 조회")
    @GetMapping
    public ResponseEntity<Page<AdminCommentListResponse>> retrieveCommentList(
            @RequestParam(name = "isReported", required = false) Boolean isReported,
            @RequestParam(name = "isReadOnlyAuthor" , required = false) Boolean isReadOnlyAuthor,
            @RequestParam(required = false) String searchOption,
            @RequestParam(required = false) String query,
             @RequestParam(defaultValue = "0") int page,
             @RequestParam(defaultValue = "25") int perPage) {

        //TODO :
        AdminCommentRetrieveCondition condition = new AdminCommentRetrieveCondition();
        condition.setIsReported(isReported);
        condition.setQuery(query);
        condition.setIsReadOnlyAuthor(isReadOnlyAuthor);
        condition.setSearchOption(searchOption);

        Page<AdminCommentListResponse> res =
                adminCommentAppService.retrieveCommentList(condition, PageRequest.of(page, perPage));
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "삭제된 댓글 목록 조회")
    @GetMapping("/deleted")
    public ResponseEntity<Page<AdminDeletedCommentResponse>> retrieveDeletedCommentList(@RequestParam(defaultValue = "0") int page,
                                                                                         @RequestParam(defaultValue = "25") int perPage) {
        Page<AdminDeletedCommentResponse> res = adminCommentAppService.retrieveDeletedCommentList(PageRequest.of(page, perPage));
        return ResponseEntity.ok(res);
    }

    //TODO : 복구 API를 벌크 버전 하나로 통합
    @Operation(summary = "댓글 복구")
    @PostMapping("/{commentId}/restore")
    public ResponseEntity<MessageResponse> restoreComment(@PathVariable Long commentId){
        adminCommentAppService.restoreComment(commentId);
        return ResponseEntity.ok(MessageResponse.of("댓글 복구 성공"));
    }

    @PostMapping("/restore")
    public ResponseEntity<MessageResponse> restoreBulkComment(@RequestBody BulkCommentRequest request){
        adminCommentAppService.restoreBulkComment(request.getCommentIds());
        return ResponseEntity.ok(MessageResponse.of("댓글 복구 성공"));
    }

    @DeleteMapping()
    public ResponseEntity<MessageResponse> deleteBulkComment(@RequestBody BulkCommentRequest request){
        adminCommentAppService.deleteBulkComment(request.getCommentIds(), false);
        return ResponseEntity.ok(MessageResponse.of("댓글 삭제 성공"));
    }

    @DeleteMapping("/permanent")
    public ResponseEntity<MessageResponse> deleteBulkCommentPermanent(@RequestBody BulkCommentRequest request){
        adminCommentAppService.deleteBulkComment(request.getCommentIds(), true);
        return ResponseEntity.ok(MessageResponse.of("댓글 영구 삭제 성공"));
    }

}
