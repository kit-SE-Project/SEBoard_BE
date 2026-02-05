package com.seproject.account.token.controller;


import com.seproject.account.token.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static com.seproject.account.token.controller.dto.TokenDTO.*;

@Tag(name = "토큰 재발행 시스템 API", description = "토큰 재발행 API")
@AllArgsConstructor
@RestController
public class RefreshTokenController {

    private final TokenService tokenService;

    @Operation(summary = "토큰 재발행", description = "refreshToken을 이용한 토큰 재 발행")
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenRefreshResponse> refreshUserToken(@RequestBody AccessTokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        AccessTokenRefreshResponse response = tokenService.refresh(refreshToken);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}
