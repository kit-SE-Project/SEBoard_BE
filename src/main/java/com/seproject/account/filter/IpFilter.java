package com.seproject.account.filter;

import com.seproject.account.common.authorize.handler.CustomAccessDeniedHandler;
import com.seproject.account.Ip.application.IpService;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomAccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class IpFilter extends OncePerRequestFilter {
    private final IpService ipService;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String address = getClientIP(request);
        String requestURI = request.getRequestURI();

        if(ipService.existSpamIp(address)) {
            accessDeniedHandler.handle(request,response,new CustomAccessDeniedException(ErrorCode.BANNED_IP,null));
        } else {
            if(requestURI.startsWith("/admin") && !ipService.existAdminIpAddress(address)) {
                accessDeniedHandler.handle(request,response,new CustomAccessDeniedException(ErrorCode.CANNOT_ACCESS_ADMIN_IP, null));
            } else {
                filterChain.doFilter(request, response);
            }
        }
    }

        public static String getClientIP(HttpServletRequest request) {
            String ip = request.getHeader("X-Forwarded-For");

            if (ip == null) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null) {
                ip = request.getRemoteAddr();
            }

            return ip;
        }
}
