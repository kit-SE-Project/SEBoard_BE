package com.seproject.admin.banned.controller;

import com.seproject.admin.banned.application.AdminBannedNicknameAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.admin.banned.controller.dto.BannedNicknameDTO.*;

@Tag(name = "금지 닉네임 관리 API", description = "관리자 시스템의 금지 닉네임 관리 API")
@AllArgsConstructor
@RequestMapping(value = "/admin/accountPolicy/bannedNickname")
@RestController
public class AdminBannedNicknameController {

    private final AdminBannedNicknameAppService bannedNicknameAppService;

    @Operation(summary = "금지 닉네임 목록 조회", description = "금지 닉네임을 조회한다.")
    @GetMapping()
    public ResponseEntity<Page<BannedNicknameResponse>> findAll(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "perPage", defaultValue = "25") int perPage) {
        Page<BannedNicknameResponse> response = bannedNicknameAppService.findAll(page,perPage);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "금지 닉네임 추가", description = "금지 닉네임을 추가한다.")
    @PostMapping()
    public ResponseEntity<Void> createBannedNickname(@RequestBody CreateBannedNicknameRequest createBannedNicknameRequest) {
        bannedNicknameAppService.createBannedNickname(createBannedNicknameRequest.getNickname());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "금지 닉네임 삭제", description = "금지 닉네임을 삭제한다.")
    @DeleteMapping()
    public ResponseEntity<Void> deleteBannedNickname(@RequestBody DeleteBannedNicknameRequest deleteBannedNicknameRequest) {
        bannedNicknameAppService.deleteBannedNickname(deleteBannedNicknameRequest.getNickname());
        return new ResponseEntity<>(HttpStatus.OK);
    }











}
