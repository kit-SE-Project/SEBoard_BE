package kr.ac.capdi.allday;

import com.nimbusds.jwt.SignedJWT;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class NoExpiryJwtTest {
    static final String KEY=Base64.getEncoder().encodeToString(new byte[32]);
    static final Instant NOW=Instant.parse("2026-10-28T00:00:00Z");
    DemoJwt jwt(Instant time,boolean permanent){return new DemoJwt(true,KEY,Clock.fixed(time,ZoneOffset.UTC),permanent,true);}
    @Test void permanentTokensHaveNoExpiryClaimAndVerifyWithoutTimeLimit() throws Exception {
        var issuer=jwt(NOW,true);
        String token=issuer.issue(3,"admin@demo.local",Set.of("ROLE_ADMIN"),"accessToken",UUID.randomUUID(),Duration.ofMinutes(30));
        var claims=SignedJWT.parse(token).getJWTClaimsSet();
        assertThat(claims.getExpirationTime()).isNull();assertThat(claims.getClaim("test_no_expiry")).isEqualTo(true);
        var verified=jwt(NOW.plus(Duration.ofDays(36525)),true).verify(token,false);
        assertThat(verified.accountId()).isEqualTo(3);assertThat(verified.noExpiry()).isTrue();assertThat(verified.expiresAt()).isNull();
    }
    @Test void ordinaryTokenExpiryStillAppliesWhenPermanentModeIsEnabled() {
        String token=jwt(NOW,false).issue(3,"admin@demo.local",Set.of("ROLE_ADMIN"),"accessToken",UUID.randomUUID(),Duration.ofMinutes(30));
        assertThatThrownBy(()->jwt(NOW.plusSeconds(1801),true).verify(token,false)).isInstanceOf(ApiProblem.class);
    }
    @Test void permanentTokensAreRejectedWhenModeIsDisabledAndTokenKindsRemainDistinct() {
        String token=jwt(NOW,true).issue(3,"admin@demo.local",Set.of("ROLE_ADMIN"),"accessToken",UUID.randomUUID(),Duration.ofMinutes(30));
        assertThatThrownBy(()->jwt(NOW,false).verify(token,false)).isInstanceOf(ApiProblem.class);
        assertThatThrownBy(()->jwt(NOW,true).verify(token,true)).isInstanceOf(ApiProblem.class);
    }
    @Test void nonExpiringModeRequiresExplicitRealTestAndDemoJwtAuthentication() {
        var clock=Clock.fixed(NOW,ZoneOffset.UTC);
        assertThatThrownBy(()->new DemoJwt(true,KEY,clock,true,false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new DemoJwt(false,KEY,clock,true,true)).isInstanceOf(IllegalStateException.class);
    }
}
