package com.seproject.allday;

import com.seproject.account.token.utils.JWTProperties;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.utils.SecurityUtils;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RequiredArgsConstructor
@RestController
public class AlldaySsoController {
    private static final String CLIENT_SECRET = "X-AllDay-Client-Secret";
    private final JwtDecoder decoder;
    private final AlldaySsoService sso;

    @PostMapping("/allday/sso/authorize")
    public ResponseEntity<?> authorize(@RequestBody Authorization input, HttpServletRequest request) {
        var token = decoder.getAccessToken(request).orElseThrow(AlldaySsoController::unauthorized);
        var account = SecurityUtils.getAccount().orElseThrow(AlldaySsoController::unauthorized);
        var signed = decoder.getSignedClaims(token);
        var claims = signed.getBody();
        long accountId = account.getAccountId();
        Object tokenAccountId = claims.get("accountId");
        if (!JWTProperties.ACCESS_TOKEN.equals(signed.getHeader().get("token_type"))
                || claims.getExpiration() == null
                || (tokenAccountId != null && !Long.toString(accountId).equals(tokenAccountId.toString()))) {
            throw unauthorized();
        }
        var url = sso.issue(accountId, claims.getExpiration().toInstant(), input.getState(), input.getCodeChallenge(), token.getToken());
        return noStore(Map.of("callbackUrl", url.toASCIIString()));
    }

    @PostMapping("/internal/allday/sso/exchange")
    public ResponseEntity<?> exchange(@RequestHeader(value = CLIENT_SECRET, required = false) String secret,
            @RequestBody Exchange input) {
        return noStore(sso.exchange(secret, input.getCode(), input.getState(), input.getCodeVerifier()));
    }

    @PostMapping("/internal/allday/sso/identity")
    public ResponseEntity<?> identity(@RequestHeader(value = CLIENT_SECRET, required = false) String secret,
            @RequestBody Identity input) {
        return noStore(sso.identity(secret, input.getGrant()));
    }

    private static ResponseEntity<?> noStore(Map<String, ?> body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    @Data
    public static class Authorization {
        private String state;
        private String codeChallenge;
    }

    @Data
    public static class Exchange {
        private String code;
        private String state;
        private String codeVerifier;
    }

    @Data
    public static class Identity {
        private String grant;
    }
}
