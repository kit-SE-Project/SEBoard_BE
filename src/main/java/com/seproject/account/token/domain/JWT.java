package com.seproject.account.token.domain;

import com.seproject.account.token.utils.JWTProperties;
import io.jsonwebtoken.Claims;
import lombok.Data;
import org.springframework.util.StringUtils;

import static com.seproject.account.token.utils.JWTProperties.*;

@Data
public class JWT {

    private String token;


    public JWT(String accessToken) {
        this.token = detach(accessToken);
    }

    private String detach(String token) {

        if (StringUtils.hasText(token) && token.startsWith(TOKEN_PREFIX)) {
            return token.substring(BEGIN_INDEX);
        }

        return token;
    }

}
