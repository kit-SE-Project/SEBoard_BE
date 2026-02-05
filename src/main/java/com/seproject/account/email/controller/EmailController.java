package com.seproject.account.email.controller;

import com.seproject.account.email.service.KumohEmailService;
import com.seproject.account.email.service.PasswordChangeEmailService;
import com.seproject.account.email.service.RegisterEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.seproject.account.email.controller.dto.EmailDTO.*;


@Tag(name = "이메일 인증 API", description = "이메일 인증(Email) 관련 API")
@RequiredArgsConstructor
@RestController
public class EmailController {

    private final RegisterEmailService registerEmailService;
    private final PasswordChangeEmailService passwordChangeEmailService;
    private final KumohEmailService kumohEmailService;

    @Operation(summary = "회원가입 이메일 인증 요청", description = "회원가입을 위하여 이메일 인증을 진행한다.")
    @PostMapping("/email/auth")
    public ResponseEntity<Void> sendEmail(@RequestBody EmailAuthenticationRequest request) {
        String email = request.getEmail();
        registerEmailService.send(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "회원가입 이메일 인증 확인", description = "회원가입을 진행하기 위해 전송한 인증 코드와 이메일이 유효한지 확인한다.")
    @PostMapping("/email/auth/confirm")
    public ResponseEntity<Void> confirmAuthCode(@RequestBody EmailConfirmRequest emailConfirmDTO) {
        registerEmailService.confirm(emailConfirmDTO.getEmail(), emailConfirmDTO.getAuthToken());
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @Operation(summary = "금오 이메일 인증 요청", description = "정회원 전환을 위하여 금오 이메일 인증을 진행한다.")
    @PostMapping("/email/kumoh")
    public ResponseEntity<Void> sendKumohEmail(@RequestBody EmailAuthenticationRequest request) {
        String email = request.getEmail();
        kumohEmailService.send(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "금오 이메일 인증 확인", description = "정회원 전환을 위해 전송한 인증 코드와 이메일이 유효한지 확인한다.")
    @PostMapping("/email/kumoh/confirm")
    public ResponseEntity<Void> confirmKumohAuthCode(@RequestBody EmailConfirmRequest emailConfirmDTO) {
        kumohEmailService.confirm(emailConfirmDTO.getEmail(), emailConfirmDTO.getAuthToken());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Operation(summary = "비밀번호 변경 이메일 인증 요청", description = "비밀번호 변경을 위하여 이메일 인증을 진행한다.")
    @PostMapping("/email/password")
    public ResponseEntity<Void> sendPasswordChangeEmail(@RequestBody EmailAuthenticationRequest request) {
        String email = request.getEmail();
        passwordChangeEmailService.send(email);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    @Operation(summary = "비밀번호 변경 이메일 인증 확인", description = "비밀번호 변경을 진행하기 위해 전송한 인증 코드와 이메일이 유효한지 확인한다.")
    @PostMapping("/email/password/confirm")
    public ResponseEntity<Void> confirmPasswordChangeAuthCode(@RequestBody EmailConfirmRequest emailConfirmDTO) {
        passwordChangeEmailService.confirm(emailConfirmDTO.getEmail(), emailConfirmDTO.getAuthToken());
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
