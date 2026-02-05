package com.seproject.account.account.controller;

import com.seproject.account.account.application.RegisterAppService;
import com.seproject.account.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.seproject.account.account.controller.dto.RegisterDTO.*;

@Tag(name = "회원가입 API", description = "회원가입(Register) 관련 API")
@RequiredArgsConstructor
@RestController
public class RegisterController {

    private final RegisterAppService registerAppService;

    private final AccountService accountService;

    @Operation(summary = "OAuth 회원가입", description = "소셜 로그인 사용시 추가 정보를 입력하여 회원가입을 요청한다.")
    @PostMapping("/account/oauth")
    public ResponseEntity<RegisterResponse> registerOAuthAccount(@RequestBody OAuth2RegisterRequest request) {
        RegisterResponse response = registerAppService.registerOAuthAccount(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }


    @Operation(summary = "Form 회원가입", description = "이메일 인증을 완료한 사용자는 회원가입 요청을 진행한다.")
    @PostMapping("/account/form")
    public ResponseEntity<RegisterResponse> registerFormAccount(@RequestBody FormRegisterRequest request) {
        RegisterResponse response = registerAppService.registerFormAccount(request);
        return new ResponseEntity<>(response,HttpStatus.OK);
    }

    @Operation(summary = "닉네임 중복 체크", description = "이미 존재하는 닉네임인지 중복 확인을 한다")
    @PostMapping("/account/nickname")
    public ResponseEntity<ConfirmDuplicateNicknameResponse> confirmDuplicateNickname(@RequestBody ConfirmDuplicateNicknameRequest confirmDuplicateNicknameRequest) {
        String nickname = confirmDuplicateNicknameRequest.getNickname();
        boolean existNickname = accountService.isExistNickname(nickname);
        return new ResponseEntity<>(ConfirmDuplicateNicknameResponse.toDTO(existNickname),HttpStatus.OK);
    }
}
