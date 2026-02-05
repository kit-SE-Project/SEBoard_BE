package com.seproject.admin;

import com.seproject.account.account.domain.Account;
import com.seproject.account.role.domain.Role;
import com.seproject.account.utils.SecurityUtils;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import com.seproject.error.exception.CustomAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Aspect
@Slf4j
public class AdminAspect {

//    @Pointcut("execution(* *..Admin*AppService.*(..))")
//    private void allAdminAppService(){}
//
//    @Around("allAdminAppService()")
//    public Object hasAdminRole(ProceedingJoinPoint joinPoint) throws Throwable {
//
//        Account account = SecurityUtils.getAccount()
//                .orElseThrow(() -> new CustomAuthenticationException(ErrorCode.NOT_LOGIN, null));
//
//        Collection<? extends GrantedAuthority> authorities = account.getAuthorities();
//
//        for (GrantedAuthority authority : authorities) {
//            if (authority.getAuthority().equals(Role.ROLE_ADMIN)) {
//
//                return joinPoint.proceed();
//            }
//        }
//
//        throw new CustomAccessDeniedException(ErrorCode.ACCESS_DENIED,null);
//
////        log.info("[@annotation] {}" , joinPoint.getSignature());
////        return joinPoint.proceed();
//    }
}
