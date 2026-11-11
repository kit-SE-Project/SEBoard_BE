package kr.ac.capdi.allday;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoAuthService {
    private final JdbcTemplate db;private final DemoJwt jwt;private final Clock clock;private final PasswordEncoder passwords;
    private final String dummyHash;private final RealTestMode mode;
    public DemoAuthService(JdbcTemplate db,DemoJwt jwt,Clock clock,PasswordEncoder passwords,RealTestMode mode){this.db=db;this.jwt=jwt;this.clock=clock;this.passwords=passwords;this.mode=mode;dummyHash=passwords.encode(UUID.randomUUID().toString());}
    Map<String,Object> account(long id){
        var rows=db.queryForList("SELECT a.account_id,a.login_id,a.name,a.status,b.board_user_id,b.member_id,b.nickname,e.last_eligibility FROM accounts a JOIN board_users b USING(account_id) JOIN external_subject_binding e USING(member_id) WHERE a.account_id=? AND e.issuer='server-allday-demo'",id);
        if(rows.isEmpty()||!"NORMAL".equals(rows.getFirst().get("status")))throw new ApiProblem(401,"ACCOUNT_UNAVAILABLE","정상 계정으로 다시 로그인하세요.");
        return rows.getFirst();
    }
    Set<String> roles(long account){return new TreeSet<>(db.queryForList("SELECT r.name FROM roles r JOIN role_accounts ra USING(role_id) WHERE ra.account_id=? ORDER BY r.name",String.class,account));}
    Actor actor(Map<String,Object> a){
        var roles=roles(Rows.number(a,"account_id"));boolean admin=roles.contains("ROLE_ADMIN");
        boolean eligible=Boolean.TRUE.equals(a.get("last_eligibility"))&&(roles.contains("ROLE_KUMOH")||admin);
        return new Actor(Rows.number(a,"member_id"),admin?"ADMIN":"STUDENT",roles,eligible);
    }
    static String hash(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    Map<String,Object> tokens(Map<String,Object> a,UUID session,String kind){
        var roles=roles(Rows.number(a,"account_id"));String login=a.get("login_id").toString();long id=Rows.number(a,"account_id");
        var life=Duration.ofSeconds(kind.equals("largeRefreshToken")?30L*24*3600:6*3600);
        String access=jwt.issue(id,login,roles,"accessToken",session,Duration.ofMinutes(30));
        String refresh=jwt.issue(id,login,roles,kind,session,life);
        db.update("INSERT INTO auth_session(session_id,account_id,refresh_hash,refresh_kind,expires_at,no_expiry) VALUES (?,?,?,?,?,?) ON CONFLICT(session_id) DO UPDATE SET refresh_hash=excluded.refresh_hash,expires_at=excluded.expires_at,no_expiry=excluded.no_expiry",session,id,hash(refresh),kind,jwt.noExpiry()?null:Rows.ts(clock.instant().plus(life)),jwt.noExpiry());
        return Map.of("accessToken",access,"refreshToken",refresh);
    }
    @Transactional(noRollbackFor=ApiProblem.class)
    public Map<String,Object> login(String username,String password,boolean large){
        jwt.configured();
        if(username==null||password==null||username.length()>254||password.getBytes(StandardCharsets.UTF_8).length>72||password.isBlank())throw ApiProblem.invalid("아이디와 비밀번호를 확인하세요. 비밀번호는 UTF-8 72바이트 이하여야 합니다.");
        String login=username.trim().toLowerCase(Locale.ROOT);if(login.isBlank())throw ApiProblem.invalid("아이디가 필요합니다.");
        var now=clock.instant();
        db.update("INSERT INTO auth_login_throttle(login_id,window_start) VALUES (?,?) ON CONFLICT DO NOTHING",login,Rows.ts(now));
        var limit=Rows.one(db,"SELECT * FROM auth_login_throttle WHERE login_id=? FOR UPDATE",login);
        var blocked=Rows.instant(limit,"blocked_until");if(blocked!=null&&blocked.isAfter(now))throw new ApiProblem(429,"LOGIN_PREVENT_USER","로그인 시도가 제한됐습니다. 10분 후 다시 시도하세요.");
        int failures=((Number)limit.get("failures")).intValue();
        if(Rows.instant(limit,"window_start").isBefore(now.minusSeconds(600))){failures=0;db.update("UPDATE auth_login_throttle SET failures=0,window_start=?,blocked_until=NULL WHERE login_id=?",Rows.ts(now),login);}
        var rows=db.queryForList("SELECT account_id,password_hash,status FROM accounts WHERE login_id=?",login);
        boolean match=passwords.matches(password,rows.isEmpty()?dummyHash:rows.getFirst().get("password_hash").toString());
        if(!match||rows.isEmpty()||!"NORMAL".equals(rows.getFirst().get("status"))){
            failures++;db.update("UPDATE auth_login_throttle SET failures=?,blocked_until=? WHERE login_id=?",failures,failures>=5?Rows.ts(now.plusSeconds(600)):null,login);
            throw new ApiProblem(401,"LOGIN_FAILED","아이디 또는 비밀번호를 확인하세요.");
        }
        db.update("DELETE FROM auth_login_throttle WHERE login_id=?",login);
        return tokens(account(Rows.number(rows.getFirst(),"account_id")),UUID.randomUUID(),large?"largeRefreshToken":"refreshToken");
    }
    Map<String,Object> session(DemoJwt.Verified token,boolean lock){
        var rows=db.queryForList("SELECT * FROM auth_session WHERE session_id=? AND account_id=?"+(lock?" FOR UPDATE":""),token.session(),token.accountId());
        if(rows.isEmpty())throw new ApiProblem(401,"SESSION_REVOKED","로그아웃되었거나 만료된 로그인입니다.");
        var row=rows.getFirst();boolean permanent=Boolean.TRUE.equals(row.get("no_expiry"));
        var expiresAt=Rows.instant(row,"expires_at");
        if(row.get("revoked_at")!=null||(token.noExpiry()&&!permanent)||(!permanent&&(expiresAt==null||!expiresAt.isAfter(clock.instant()))))
            throw new ApiProblem(401,"SESSION_REVOKED","로그아웃되었거나 만료된 로그인입니다.");
        return row;
    }
    @Transactional(readOnly=true) public Actor authenticate(String raw){var token=jwt.verify(raw,false);session(token,false);return actor(account(token.accountId()));}
    @Transactional public Object refresh(String raw){
        var token=jwt.verify(raw,true);var s=session(token,true);
        if(!hash(raw).equals(s.get("refresh_hash"))||!token.kind().equals(s.get("refresh_kind")))throw new ApiProblem(401,"REFRESH_REUSED","이미 사용한 갱신 토큰입니다.");
        return tokens(account(token.accountId()),token.session(),token.kind());
    }
    @Transactional public Object logout(String access,String refresh){
        var a=jwt.verify(access,false);var r=jwt.verify(refresh,true);var s=session(a,true);
        if(a.accountId()!=r.accountId()||!a.session().equals(r.session())||!hash(refresh).equals(s.get("refresh_hash")))throw new ApiProblem(401,"INVALID_REFRESH_TOKEN","현재 로그인에 속한 갱신 토큰이 필요합니다.");
        db.update("UPDATE auth_session SET revoked_at=? WHERE session_id=?",Rows.ts(clock.instant()),a.session());
        return Map.of("isOAuth",false,"loggedOut",true);
    }
    @Transactional(readOnly=true) public Object me(String raw){
        var token=jwt.verify(raw,false);session(token,false);var a=account(token.accountId());var actor=actor(a);
        return Map.of("accountId",token.accountId(),"userId",Rows.number(a,"board_user_id"),"memberId",actor.memberId(),"nickname",a.get("nickname"),"email",a.get("login_id"),"roles",actor.authorities(),"rentalEligible",actor.rentalEligible(),"mode",mode.name());
    }
}
