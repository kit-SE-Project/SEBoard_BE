package kr.ac.capdi.allday;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DemoJwt {
    static final String ISSUER="server-allday-demo";
    private final byte[] key; private final Clock clock; private final boolean enabled,noExpiry;
    public DemoJwt(@Value("${allday.demo-auth-enabled}") boolean enabled,@Value("${allday.demo-jwt-key}") String key,Clock clock,
        @Value("${allday.test-auth-no-expiry:false}") boolean noExpiry,@Value("${allday.real-test-enabled:false}") boolean realTest){
        this.enabled=enabled;this.clock=clock;this.noExpiry=noExpiry;
        if(noExpiry&&(!enabled||!realTest))throw new IllegalStateException("무기한 시험 토큰은 REAL_TEST와 데모 JWT 인증을 함께 사용하는 환경에서만 켤 수 있습니다.");
        try{this.key=key.isBlank()?new byte[0]:Base64.getDecoder().decode(key);}catch(IllegalArgumentException e){throw new IllegalStateException("DEMO_JWT_KEY는 base64여야 합니다.");}
        if(enabled&&this.key.length!=32)throw new IllegalStateException("데모 JWT에 별도의 32바이트 DEMO_JWT_KEY가 필요합니다.");
    }
    public void configured(){if(!enabled)throw new ApiProblem(503,"AUTH_NOT_CONFIGURED","데모 로그인은 데모 인증 환경에서만 사용할 수 있습니다.");}
    boolean noExpiry(){return noExpiry;}
    String issue(long account,String login,Collection<String> roles,String kind,UUID session,Duration life){
        configured();var now=clock.instant();
        try{
            var builder=new JWTClaimsSet.Builder().issuer(ISSUER).subject(login).issueTime(Date.from(now))
                .jwtID(UUID.randomUUID().toString()).claim("accountId",account).claim("authorities",roles).claim("sid",session.toString());
            if(noExpiry)builder.claim("test_no_expiry",true);
            else builder.expirationTime(Date.from(now.plus(life)));
            var claims=builder.build();
            var jwt=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).customParam("token_type",kind).build(),claims);
            jwt.sign(new MACSigner(key));return jwt.serialize();
        }catch(Exception e){throw new IllegalStateException("JWT 발급 실패");}
    }
    record Verified(long accountId,UUID session,String tokenId,String kind,Instant expiresAt,boolean noExpiry){}
    Verified verify(String raw,boolean refresh){
        configured();
        try{
            if(raw==null||raw.length()>8192)throw new IllegalArgumentException();
            var jwt=SignedJWT.parse(raw);
            if(!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())||!jwt.verify(new MACVerifier(key)))throw new IllegalArgumentException();
            String kind=(String)jwt.getHeader().getCustomParam("token_type");
            if(refresh?!Set.of("refreshToken","largeRefreshToken").contains(kind):!"accessToken".equals(kind))throw new IllegalArgumentException();
            var c=jwt.getJWTClaimsSet();var now=clock.instant();
            boolean permanent=Boolean.TRUE.equals(c.getClaim("test_no_expiry"));
            if(permanent) {
                if(!noExpiry||c.getExpirationTime()!=null)throw new IllegalArgumentException();
            } else if(c.getExpirationTime()==null||!c.getExpirationTime().toInstant().isAfter(now))throw new IllegalArgumentException();
            if(!ISSUER.equals(c.getIssuer())||c.getIssueTime()==null||c.getIssueTime().toInstant().isAfter(now.plusSeconds(5))||c.getJWTID()==null)throw new IllegalArgumentException();
            return new Verified(c.getLongClaim("accountId"),UUID.fromString(c.getStringClaim("sid")),c.getJWTID(),kind,c.getExpirationTime()==null?null:c.getExpirationTime().toInstant(),permanent);
        }catch(Exception e){throw new ApiProblem(401,"INVALID_OR_EXPIRED_TOKEN","토큰의 종류·서명·만료를 확인하세요.");}
    }
}
