package com.seproject.admin.settings.controller;

import com.seproject.admin.settings.application.LoginSettingAppService;
import com.seproject.admin.settings.controller.dto.LoginSettingDTO;
import com.seproject.admin.settings.service.LoginSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "로그인 옵션 관리 API", description = "로그인 옵션 관리 API")
@RequiredArgsConstructor
@RequestMapping(value = "/admin/loginSettings")
@RestController
public class LoginSettingController {

    private final LoginSettingAppService loginSettingAppService;
    @Operation(summary = "로그인 제한 시간, 시도 횟수 조회")
    @GetMapping
    public ResponseEntity<LoginSettingDTO> findLoginSetting() {
        LoginSettingDTO response = loginSettingAppService.getLoginSetting();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "로그인 제한 시간, 시도 횟수 수정")
    @PutMapping
    public ResponseEntity<Void> updateLoginSetting(@RequestBody LoginSettingDTO request) {
        loginSettingAppService.update(request);
        return new ResponseEntity<>(HttpStatus.OK);

    }

}
