package com.seproject.admin.banned.controller;

import com.seproject.admin.banned.application.AdminBannedIdAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.admin.banned.controller.dto.BannedIdDTO.*;

@Tag(name = "금지 아이디 관리 API", description = "관리자 시스템의 금지 아이디 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/accountPolicy/bannedId")
@RestController
public class AdminBannedIdController {

    private final AdminBannedIdAppService bannedIdAppService;

    @Operation(summary = "금지 아이디 목록 조회", description = "금지 아이디를 조회한다.")
    @GetMapping()
    public ResponseEntity<Page<BannedIdResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "perPage", defaultValue = "25") int perPage) {
        Page<BannedIdResponse> response = bannedIdAppService.findAll(page, perPage);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "금지 아이디 추가", description = "금지 아이디를 추가한다.")
    @PostMapping()
    public ResponseEntity<Void> createBannedId(@RequestBody CreateBannedIdRequest request) {
        bannedIdAppService.create(request.getBannedId());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "금지 아이디 삭제", description = "금지 아이디를 삭제한다.")
    @DeleteMapping()
    public ResponseEntity<Void> deleteBannedId(@RequestBody DeleteBannedIdRequest request) {
        bannedIdAppService.delete(request.getBannedId());
        return new ResponseEntity<>(HttpStatus.OK);
    }




}
