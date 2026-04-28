package com.seproject.admin.tier.controller;

import com.seproject.member.application.TierBatchService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tier")
@RequiredArgsConstructor
public class AdminTierController {

    private final TierBatchService tierBatchService;

    @Operation(summary = "티어 배치 수동 실행 (관리자)")
    @PostMapping("/batch")
    public ResponseEntity<String> runTierBatch() {
        tierBatchService.updateTiersAndGrantFrames();
        return ResponseEntity.ok("티어 배치 완료");
    }
}
