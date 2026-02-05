package com.seproject.board.common.controller;

import com.seproject.board.common.application.ReportAppService;
import com.seproject.board.common.controller.dto.MessageResponse;
import com.seproject.board.common.controller.dto.ReportThresholdRequest;
import com.seproject.board.common.controller.dto.ReportThresholdResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ReportController {
    private final ReportAppService reportAppService;
    @PostMapping("/admin/report/threshold")
    public ResponseEntity<MessageResponse> setReportThreshold(@RequestBody @Validated ReportThresholdRequest request) {
        reportAppService.setReportThreshold(request);
        return ResponseEntity.ok(MessageResponse.of("신고 임계치 설정 완료"));
    }

    @GetMapping("/admin/report/threshold")
    public ResponseEntity<ReportThresholdResponse> retrieveReportThreshold(){
        return ResponseEntity.ok(reportAppService.retrieveReportThreshold());
    }

    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<MessageResponse> reportPost(@PathVariable Long postId) {
        reportAppService.reportPost(postId);
        return ResponseEntity.ok(MessageResponse.of("신고 완료"));
    }

    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<MessageResponse> reportComment(@PathVariable Long commentId) {
        reportAppService.reportComment(commentId);
        return ResponseEntity.ok(MessageResponse.of("신고 완료"));
    }
}
