package com.seproject.account.token.utils;

public class JWTProperties {
    public static final Integer BEGIN_INDEX = 7;
    public static final String TYPE = "typ";
    public static final String ALGORITHM = "alg";
    public static final String HS256 = "HS256";
    public static final String AUTHORITIES = "authorities";

    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String ACCESS_TOKEN = "accessToken";
    public static final String LARGE_REFRESH_TOKEN = "largeRefreshToken";

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_NAME = "Authorization";

    public static final Integer ACCESS_TOKEN_EXPIRE = 30*60;
    public static final Integer REFRESH_TOKEN_EXPIRE = 6*60*60;
    public static final Integer LARGE_REFRESH_TOKEN_EXPIRE = 30*24*60*60;

}
