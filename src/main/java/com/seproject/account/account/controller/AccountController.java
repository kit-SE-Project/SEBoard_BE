package com.seproject.account.account.controller;

import com.seproject.account.account.application.AccountAppService;
import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoChangeRequest;
import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoChangeResponse;
import com.seproject.account.account.controller.dto.MyPageDTO.MyInfoResponse;
import com.seproject.account.account.controller.dto.WithDrawDTO.WithDrawResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.account.account.controller.dto.KumohAuthDTO.*;
import static com.seproject.account.account.controller.dto.LogoutDTO.*;
import static com.seproject.account.account.controller.dto.PasswordDTO.*;
import static com.seproject.account.account.controller.dto.WithDrawDTO.*;

@Tag(name = "계정 시스템 API", description = "계정(Account) 관련 API")
@RequiredArgsConstructor
@RestController
public class AccountController {

    private final AccountAppService accountAppService;

    @Operation(summary = "로그아웃", description = "로그아웃")
    @PostMapping("/logoutProc")
    public ResponseEntity<LogoutResponseDTO> logout(@RequestBody LogoutRequestDTO request) {
        LogoutResponseDTO responseDTO = accountAppService.logout(request);
        return new ResponseEntity<>(responseDTO, HttpStatus.OK);
    }

    @Operation(summary = "회원 탈퇴", description = "회원 탈퇴를 시도")
    @DeleteMapping("/withdraw") // 응답 필드 : 닉네임 삭제
    public ResponseEntity<WithDrawResponse> withdraw(@RequestBody WithDrawRequest request) {
        WithDrawResponse response = accountAppService.withDraw(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "비밀번호 찾기", description = "아이디를 이용하여 비밀번호 변경")
    @PostMapping("/password")
    public ResponseEntity<ResetPasswordResponse> changePassword(@RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = accountAppService.resetPassword(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "금오인 인증", description = "금오 이메일 인증 후 전환")
    @PostMapping("/kumoh")
    public ResponseEntity<KumohAuthResponse> addKumohRole(@RequestBody KumohAuthRequest request) {
        KumohAuthResponse response = accountAppService.addKumohRole(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "내 정보 조회", description = "마이페이지에 필요한 정보 조회")
    @GetMapping("/mypage")
    public ResponseEntity<MyInfoResponse> findMyPage() {
        MyInfoResponse response = accountAppService.findMyPage();
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "내정보 변경", description = "마이페이지에서 내 정보를 변경함")
    @PutMapping("/mypage/info")
    public ResponseEntity<MyInfoChangeResponse> changeMyInfo(@RequestBody MyInfoChangeRequest request) {
        MyInfoChangeResponse response = accountAppService.updateMyInfo(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }


    @Operation(summary = "비밀번호 변경", description = "마이페이지에서 비밀번호를 변경함")
    @PostMapping("/mypage/password")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        accountAppService.changePassword(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
