package com.seproject.oauth2.controller;

import com.seproject.oauth2.application.LogoutAppService;
import com.seproject.oauth2.model.PrincipalUser;
import com.seproject.oauth2.service.OAuth2LoginService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.seproject.oauth2.controller.dto.RegisterDTO.*;

@AllArgsConstructor
@Controller
public class AccountController {

    private final LogoutAppService logoutAppService;
    private final OAuth2LoginService oAuth2LoginService;

    @PostMapping("/register/oauth2")
    public ResponseEntity<?> registerUserWithOAuth(@AuthenticationPrincipal PrincipalUser principalUser, @RequestBody OAuth2RegisterRequest oAuth2RegisterRequest) {

        if(principalUser == null) {
            return new ResponseEntity<>("회원 정보가 없어 접근 불가능",HttpStatus.BAD_REQUEST);
        }

        try{
            oAuth2LoginService.registerUser(principalUser.getProviderUser(),oAuth2RegisterRequest.getNickname());
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e,HttpStatus.BAD_REQUEST);
        }

        return new ResponseEntity<>("회원가입 완료",HttpStatus.OK);
    }

    @GetMapping("/logoutProc")
    public ResponseEntity<?> logout(Authentication authentication) {
        System.out.println("----호출----");
        if(authentication == null) {
            return new ResponseEntity<>("로그인 상태가 아닙니다.",HttpStatus.BAD_REQUEST);
        }

        if(authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken)authentication;
            logoutAppService.logout(token.getAuthorizedClientRegistrationId());

            return new ResponseEntity<>("로그아웃 성공", HttpStatus.OK);
        }

        return null;
    }
}