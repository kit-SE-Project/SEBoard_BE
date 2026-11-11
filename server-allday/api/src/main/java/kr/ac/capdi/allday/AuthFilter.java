package kr.ac.capdi.allday;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Component @Order(2)
public class AuthFilter extends OncePerRequestFilter {
    private final boolean enabled;
    private final String one,two,admin,worker;
    private final ObjectMapper json;
    private final boolean demo;private final DemoAuthService demoAuth;
    private final SeBoardAuthentication seboard;
    public AuthFilter(@Value("${allday.dev-auth-enabled}") boolean enabled,
        @Value("${allday.student-one-token}") String one,@Value("${allday.student-two-token}") String two,
        @Value("${allday.admin-token}") String admin,@Value("${allday.worker-token}") String worker,ObjectMapper json,
        @Value("${allday.demo-auth-enabled}") boolean demo,DemoAuthService demoAuth,SeBoardAuthentication seboard) {
        this.demo=demo;this.demoAuth=demoAuth;this.seboard=seboard;
        if(seboard.enabled()&&(demo||enabled))throw new IllegalStateException("SEBoard 인증은 데모 또는 개발 인증과 함께 켤 수 없습니다.");
        if(demo&&enabled)throw new IllegalStateException("데모 JWT 인증과 개발 고정 토큰 인증은 함께 켤 수 없습니다.");
        this.enabled=enabled;this.one=one;this.two=two;this.admin=admin;this.worker=worker;this.json=json;
        if(enabled && (one.length()<24 || two.length()<24 || admin.length()<24 ||
            Set.of(one,two,admin).size()!=3)) throw new IllegalStateException("개발 토큰은 서로 다른 24자 이상의 값이어야 합니다.");
        if(!worker.isEmpty() && (worker.length()<24 || List.of(one,two,admin).contains(worker)))
            throw new IllegalStateException("worker 토큰은 사용자 토큰과 구분해야 합니다.");
    }
    private boolean matches(String input,String expected) {
        return !expected.isEmpty() && MessageDigest.isEqual(input.getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8));
    }
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        String requestId=UUID.randomUUID().toString();req.setAttribute("requestId",requestId);res.setHeader("X-Request-Id",requestId);
        res.setHeader("Cache-Control","no-store");res.setHeader("X-Content-Type-Options","nosniff");
        if(Set.of("/health","/formLogin","/refresh").contains(req.getRequestURI())||req.getMethod().equals("OPTIONS")) {chain.doFilter(req,res);return;}
        String header=req.getHeader("Authorization");String token=header!=null&&header.startsWith("Bearer ")?header.substring(7):"";
        Actor actor=null;
        boolean internal=req.getRequestURI().startsWith("/api/internal/");
        if(internal && matches(token,worker)) actor=new Actor(0,"WORKER");
        else if(!internal&&(demo||seboard.enabled())) {
            try{actor=seboard.enabled()?seboard.authenticate(token):demoAuth.authenticate(token);}
            catch(ApiProblem e){reject(res,e.status,e.code,e.getMessage(),requestId);return;}
            catch(org.springframework.dao.DataAccessException e){reject(res,503,"AUTH_STORE_UNAVAILABLE","인증 저장소를 사용할 수 없습니다.",requestId);return;}
        }
        else if(!internal&&enabled) {
            if(matches(token,one)) actor=new Actor(1001,"STUDENT");
            else if(matches(token,two)) actor=new Actor(1002,"STUDENT");
            else if(matches(token,admin)) actor=new Actor(9001,"ADMIN");
        }
        if(actor==null) {
            int status=(!internal&&!enabled&&!demo&&!seboard.enabled())?503:401;
            res.setStatus(status);res.setContentType("application/json;charset=UTF-8");
            json.writeValue(res.getOutputStream(),ApiErrors.body(status,status==503?"AUTH_NOT_CONFIGURED":"UNAUTHENTICATED",
                status==503?"학과 인증 연결 전에는 개발 인증 설정이 필요합니다.":"유효한 인증 토큰이 필요합니다.",requestId));return;
        }
        req.setAttribute("actor",actor);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,actor.authorities().stream().map(SimpleGrantedAuthority::new).toList()));
        chain.doFilter(req,res);
    }
    private void reject(HttpServletResponse res,int status,String code,String message,String requestId)throws IOException{
        res.setStatus(status);res.setContentType("application/json;charset=UTF-8");json.writeValue(res.getOutputStream(),ApiErrors.body(status,code,message,requestId));
    }
}
