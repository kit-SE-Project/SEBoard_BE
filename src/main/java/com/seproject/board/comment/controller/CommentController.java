package com.seproject.board.comment.controller;

import com.seproject.board.comment.application.CommentAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.comment.controller.dto.CommentRequest.CreateCommentRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.seproject.board.common.controller.dto.MessageResponse.*;
import static com.seproject.board.comment.controller.dto.CommentRequest.*;

@AllArgsConstructor
@Tag(name = "댓글 API", description = "댓글(comments) 관련 API")
@RequestMapping("/comments")
@RestController
public class CommentController {
    private final CommentAppService commentAppService;

    @Parameter(name = "request", description = "댓글 달려고 하는 게시글의 pk, 댓글 내용을 전달")
    @Operation(summary = "댓글 작성", description = "사용자는 실명으로 댓글을 작성한다.")
    @PostMapping()
    public ResponseEntity<?> createComment(@Validated @RequestBody CreateCommentRequest request) {
        Long commentId = commentAppService.writeComment(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateAndUpdateMessage.of(commentId, "댓글 작성 성공"));
    }


    @Parameters(
            {
                    @Parameter(name = "commentId", description = "수정할 댓글의 pk"),
                    @Parameter(name = "contents", description = "수정할 내용")
            }
    )
    @Operation(summary = "댓글 수정", description = "사용자는 자신이 작성한 댓글을 수정한다.")
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId, @Validated @RequestBody UpdateCommentRequest request) {

        Long id = commentAppService.editComment(
                request.toCommand(commentId)
        );

        return ResponseEntity.ok(CreateAndUpdateMessage.of(id, "댓글 수정 성공"));
    }

    @Parameter(name = "commentId", description = "삭제할 댓글의 pk")
    @Operation(summary = "댓글 삭제", description = "사용자는 자신이 작성한 댓글을 삭제한다")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteNamedComment(@PathVariable Long commentId) {

        commentAppService.removeComment(commentId);

        return ResponseEntity.ok(MessageResponse.of("댓글 삭제 성공"));
    }
}
