package com.seproject.allday;

import com.seproject.account.token.utils.JWTProperties;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.utils.SecurityUtils;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class AlldayIdentityController {
    private final JwtDecoder decoder;

    @GetMapping("/internal/allday/identity")
    public ResponseEntity<?> identity(HttpServletRequest request) {
        var account = SecurityUtils.getAccount();
        var token = decoder.getAccessToken(request);
        if (account.isEmpty() || token.isEmpty()) {
            return unauthorized();
        }
        var signed = decoder.getSignedClaims(token.get());
        var claims = signed.getBody();
        long accountId = account.get().getAccountId();
        Object tokenAccountId = claims.get("accountId");
        if (!JWTProperties.ACCESS_TOKEN.equals(signed.getHeader().get("token_type"))
                || claims.getExpiration() == null
                || (tokenAccountId != null && !Long.toString(accountId).equals(tokenAccountId.toString()))) {
            return unauthorized();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "accountId", accountId,
                "authorities", account.get().getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList())));
    }

    private ResponseEntity<Void> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore()).build();
    }
}
