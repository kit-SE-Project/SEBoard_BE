package com.seproject.board.comment.controller;

import com.seproject.board.comment.application.ReplyAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.comment.controller.dto.ReplyRequest.CreateReplyRequest;
import com.seproject.board.comment.controller.dto.ReplyRequest.UpdateReplyRequest;
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

@Tag(name = "답글 API", description = "답글(reply) 관련 API")
@AllArgsConstructor
@RequestMapping("/reply")
@RestController
public class ReplyController {
    private final ReplyAppService replyAppService;

    @Parameter(name = "request", description = "답글을 달 댓글의 pk, 태그할 댓글(또는 답글)의 pk, 태그 작성자 정보, 본문, 익명 작성자 정보를 전달")
    @Operation(summary = "답글 작성", description = "답글을 작성한다")
    @PostMapping()
    public ResponseEntity<?> createReply(@Validated @RequestBody CreateReplyRequest request) {

        Long replyId = replyAppService.writeReply(request.toCommand());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CreateAndUpdateMessage.of(replyId, "답글 작성 완료"));
    }

    @Parameters(
            {
                    @Parameter(name = "replyId", description = "수정할 댓글의 pk"),
                    @Parameter(name = "request", description = "수정된 본문, 댓글의 비밀번호 정보")
            }
    )
    @Operation(summary = "답글 수정", description = "답글을 수정한다")
    @PutMapping("/{replyId}")
    public ResponseEntity<?> updateReply(@PathVariable Long replyId , @Validated @RequestBody UpdateReplyRequest request) {
        Long id = replyAppService.editReply(request.toCommand(replyId));

        return ResponseEntity.ok(CreateAndUpdateMessage.of(id, "답글 수정 완료"));
    }

    @Parameters(
            {
                    @Parameter(name = "replyId", description = "삭제할 게시글 pk"),
            }
    )
    @Operation(summary = "답글 삭제", description = "답글을 삭제한다")
    @DeleteMapping("/{replyId}")
    public ResponseEntity<?> deleteReply(@PathVariable Long replyId) {
        replyAppService.removeReply(replyId);

        return ResponseEntity.ok(MessageResponse.of("답글 삭제 완료"));
    }
}
