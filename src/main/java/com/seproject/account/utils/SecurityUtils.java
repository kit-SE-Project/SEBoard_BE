package com.seproject.account.utils;

import com.seproject.account.account.domain.Account;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Optional;

public class SecurityUtils {


    /**
     *  1. 필터에 적용된 구간
     *  HttpServletRequest -> getHeader("Authorization") -> String으로 된 jwt를 획득
     *  redis의 토큰 저장소에 토큰 존재 여부 확인 -> 저장된 토큰이 아님(우리가 발행하지 않았거나, 로그아웃 토큰, 만료 토큰 등)
     *  jwtDecoder -> Subject 파싱 -> loginId 획득 -> AccountService로 사용자를 데이터베이스에서 조회
     *  User 객체 생성(loginId => getUsername() , 권한 정보 => getAuthorities()) -> UsernamePassword 토큰으로 SecurityContext에 저장, password는 빈 문자열 저장
     *  ------------------------------------------------------------------------------------------------------------------------------------
     *  2. 이후 필요할때 꺼내쓰는 방법
     *  SecurityContextHolder.getContext().getAuthentication(); -> 저장된 UsernamePassword 토큰 꺼내기,로그인 안한 사용자면 null이 반환 밑에 코드로 참고
     *  토큰의 getPrincipal() 메소드는 사용자 정보, 권한을 가진 User객체 반환
     *  User getUsername() -> loginId 조회 -> AccountService를 이용해서 Account 객체 조회
     *
     *  getLoginId() -> 사용자의 로그인 아이디 조회
     *  getAuthorities() -> 사용자가 가진 권한 조회
     *
     *  헤더에 Authorization : 토큰 값 넣고 -> http://localhost:8080/parseAccount -> 결과
     *
     */


    public static String getLoginId() {
        Account account = getAccount().orElse(null);
        if(account == null) return null;
        return account.getUsername();
    }

    public static Collection<? extends GrantedAuthority> getAuthorities(){
        Account account = getAccount().orElse(null);
        if(account == null) return null;
        return account.getAuthorities();
    }

    public static Optional<Account> getAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null) return Optional.ofNullable(null);

        if (authentication instanceof AnonymousAuthenticationToken) {
            return Optional.ofNullable(null);
        }

        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = (UsernamePasswordAuthenticationToken) authentication;
        return Optional.of((Account)usernamePasswordAuthenticationToken.getPrincipal());
    }
}
