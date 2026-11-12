package com.seproject.allday;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.member.domain.Member;
import com.seproject.member.domain.repository.MemberRepository;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AlldaySsoService {
    private static final Duration ACCESS_LIFETIME = Duration.ofMinutes(30);
    private static final Duration CODE_LIFETIME = Duration.ofSeconds(60);
    private static final Duration GRANT_LIFETIME = Duration.ofHours(8);
    private static final String PREFIX = "allday:sso:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final URI callback;
    private final byte[] clientSecret;
    private final ValueOperations<String, String> values;
    private final ObjectMapper json;
    private final AccountRepository accounts;
    private final MemberRepository members;
    private final LogoutTokenRepository revokedTokens;

    public AlldaySsoService(@Value("${allday.sso.callback-url:}") String callbackUrl,
            @Value("${allday.sso.client-secret:}") String secret, StringRedisTemplate redis,
            ObjectMapper json, AccountRepository accounts, MemberRepository members,
            LogoutTokenRepository revokedTokens) {
        callback = callbackUrl.isBlank() ? null : URI.create(callbackUrl);
        clientSecret = secret.getBytes(StandardCharsets.UTF_8);
        if (callback != null) {
            boolean secure = "https".equals(callback.getScheme());
            boolean local = "http".equals(callback.getScheme()) && "127.0.0.1".equals(callback.getHost());
            if ((!secure && !local) || callback.getHost() == null || callback.getUserInfo() != null
                    || callback.getQuery() != null || callback.getFragment() != null
                    || !"/api/v1/sso/callback".equals(callback.getRawPath()) || clientSecret.length < 32) {
                throw new IllegalArgumentException("Configure a fixed AllDay callback and a dedicated SSO secret of at least 32 bytes.");
            }
        } else if (clientSecret.length != 0) {
            throw new IllegalArgumentException("Configure ALLDAY_SSO_CALLBACK_URL when enabling AllDay SSO.");
        }
        this.values = redis.opsForValue();
        this.json = json;
        this.accounts = accounts;
        this.members = members;
        this.revokedTokens = revokedTokens;
    }

    public URI issue(long accountId, Instant accessExpires, String state, String challenge, String accessToken) {
        requireEnabled();
        requireIdentifier(state);
        requireIdentifier(challenge);
        Duration remaining = Duration.between(Instant.now(), accessExpires);
        if (remaining.isNegative() || remaining.isZero() || remaining.compareTo(ACCESS_LIFETIME) > 0) throw unauthorized();
        String code = random();
        String generationKey = generationKey(accountId);
        String generation = values.get(generationKey);
        if (generation == null) {
            values.setIfAbsent(generationKey, random());
            generation = values.get(generationKey);
        }
        if (generation == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        var grant = new Grant(accountId, accessExpires.toEpochMilli(), generation, state, challenge);
        if (revokedTokens.existsById(accessToken)) throw unauthorized();
        Duration ttl = remaining.compareTo(CODE_LIFETIME) < 0 ? remaining : CODE_LIFETIME;
        if (ttl.toMillis() < 1 || !Boolean.TRUE.equals(values.setIfAbsent(key("code", code), encode(grant), ttl))) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        return UriComponentsBuilder.fromUri(callback).queryParam("code", code).queryParam("state", state).build().toUri();
    }

    public Map<String, Object> exchange(String secret, String code, String state, String verifier) {
        requireClient(secret);
        requireIdentifier(code);
        requireIdentifier(state);
        requireIdentifier(verifier);
        String key = key("code", code);
        String stored = values.get(key);
        Grant grant = decode(stored);
        if (!state.equals(grant.getState()) || !hash(verifier).equals(grant.getChallenge())) throw unauthorized();

        // GETDEL makes redemption atomic across SEBoard instances.
        if (!Objects.equals(stored, values.getAndDelete(key))) throw unauthorized();
        Map<String, Object> result = identity(grant);
        String grantId = random();
        grant.setExpiresAt(Instant.now().plus(GRANT_LIFETIME).toEpochMilli());
        grant.setState(null);
        grant.setChallenge(null);
        values.set(key("grant", grantId), encode(grant), GRANT_LIFETIME);
        result.put("grant", grantId);
        result.put("expiresAt", grant.getExpiresAt());
        return result;
    }

    public Map<String, Object> identity(String secret, String grantId) {
        requireClient(secret);
        requireIdentifier(grantId);
        return identity(decode(values.get(key("grant", grantId))));
    }

    public void revokeAccount(long accountId) {
        if (callback != null) {
            values.setIfPresent(generationKey(accountId), random());
        }
    }

    private Map<String, Object> identity(Grant grant) {
        long accountId = grant.getAccountId();
        String currentGeneration = values.get(generationKey(accountId));
        if (grant.getExpiresAt() <= System.currentTimeMillis()
                || currentGeneration == null || !currentGeneration.equals(grant.getGeneration())) {
            throw unauthorized();
        }
        var account = accounts.findActiveByIdWithRole(accountId).orElseThrow(AlldaySsoService::unauthorized);
        var result = new LinkedHashMap<String, Object>();
        result.put("accountId", accountId);
        result.put("authorities", account.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        result.put("nickname", members.findByAccountId(accountId).map(Member::getName).orElse(""));
        return result;
    }

    // A missing generation invalidates earlier grants, even after Redis eviction.
    private static String generationKey(long accountId) {
        return PREFIX + "generation:" + accountId;
    }

    private void requireEnabled() {
        if (callback == null) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private void requireClient(String secret) {
        requireEnabled();
        if (secret == null || !MessageDigest.isEqual(clientSecret, secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static void requireIdentifier(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{43}")) throw unauthorized();
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }

    private Grant decode(String value) {
        if (value == null) throw unauthorized();
        try {
            return json.readValue(value, Grant.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private String encode(Grant grant) {
        try {
            return json.writeValueAsString(grant);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot store AllDay grant", e);
        }
    }

    private static String random() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String key(String type, String value) {
        return PREFIX + type + ":" + hash(value);
    }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Grant {
        private long accountId;
        private long expiresAt;
        private String generation;
        private String state;
        private String challenge;
    }
}
