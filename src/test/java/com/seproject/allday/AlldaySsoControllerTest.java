package com.seproject.allday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.OAuthAccount;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.account.domain.repository.OAuthAccountRepository;
import com.seproject.account.common.authentication.handler.failure.CustomAuthenticationFailureHandler;
import com.seproject.account.filter.JwtFilter;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.account.token.domain.repository.LogoutLargeRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutRefreshTokenRepository;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.account.token.service.TokenService;
import com.seproject.account.token.utils.JWTProperties;
import com.seproject.account.token.utils.JwtDecoder;
import com.seproject.account.token.utils.JwtProvider;
import com.seproject.account.utils.ErrorCodeConverter;
import com.seproject.account.utils.ResponseWriter;
import com.seproject.board.common.Status;
import com.seproject.member.domain.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlldaySsoControllerTest {
    private static final String AUTHORIZE = "/allday/sso/authorize";
    private static final String CALLBACK = "https://rental.example/api/v1/sso/callback";
    private static final String SECRET = "private-sso-fixture-secret-at-least-32-bytes";
    private static final String STATE = "s".repeat(43);
    private static final String VERIFIER = "v".repeat(43);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final OAuthAccountRepository oauth = mock(OAuthAccountRepository.class);
    private final LogoutTokenRepository revoked = mock(LogoutTokenRepository.class);
    private final JwtProvider provider = new JwtProvider();
    private final Map<String, String> records = new ConcurrentHashMap<>();
    private final ObjectMapper json = new ObjectMapper();
    private AlldaySsoService sso;
    private TokenService tokens;
    private JwtDecoder decoder;
    private MockMvc mvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        decoder = new JwtDecoder(accounts);
        ReflectionTestUtils.setField(provider, "secret", secret);
        ReflectionTestUtils.setField(decoder, "secret", secret);
        tokens = new TokenService(accounts, oauth, provider, decoder,
                mock(LogoutRefreshTokenRepository.class), mock(LogoutLargeRefreshTokenRepository.class));
        var failures = new CustomAuthenticationFailureHandler(new ResponseWriter(), new ErrorCodeConverter());
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(args -> records.get(args.getArgument(0)));
        when(values.getAndDelete(anyString())).thenAnswer(args -> records.remove(args.getArgument(0)));
        when(values.setIfAbsent(anyString(), anyString())).thenAnswer(args ->
                records.putIfAbsent(args.getArgument(0), args.getArgument(1)) == null);
        when(values.setIfPresent(anyString(), anyString())).thenAnswer(args ->
                records.replace(args.getArgument(0), args.getArgument(1)) != null);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(args ->
                records.putIfAbsent(args.getArgument(0), args.getArgument(1)) == null);
        doAnswer(args -> records.put(args.getArgument(0), args.getArgument(1))).when(values)
                .set(anyString(), anyString(), any(Duration.class));
        sso = new AlldaySsoService(CALLBACK, SECRET, redis, json, accounts, members, revoked);
        mvc = MockMvcBuilders.standaloneSetup(new AlldaySsoController(decoder, sso))
                .addFilters(new RequestContextFilter(), new SecurityContextPersistenceFilter(),
                        new JwtFilter(tokens, failures, revoked))
                .build();
    }

    @Test
    void requiresCurrentBoardAccessTokenAndPinsCallback() throws Exception {
        var account = account(83, "ROLE_ADMIN");
        var authentication = new UsernamePasswordAuthenticationToken(account, "", account.getAuthorities());
        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.of(account));
        String access = provider.createAccessToken(authentication).getToken();
        String refresh = provider.createRefreshToken(authentication).getToken();
        String expired = provider.createToken(authentication, JWTProperties.ACCESS_TOKEN, JWTProperties.HS256,
                Date.from(Instant.now().minusSeconds(60)), Date.from(Instant.now().minusSeconds(30))).getToken();

        mvc.perform(post(AUTHORIZE).contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isUnauthorized());
        mvc.perform(authorize(refresh)).andExpect(status().isUnauthorized());
        mvc.perform(authorize(expired)).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(108));
        mvc.perform(authorize(access + "bad")).andExpect(status().isUnauthorized());

        var result = mvc.perform(authorize(access)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        String callback = json.readTree(result.getResponse().getContentAsString()).path("callbackUrl").asText();
        assertTrue(callback.startsWith(CALLBACK + "?code="));
        assertFalse(callback.contains(access));
        assertEquals(STATE, UriComponentsBuilder.fromUriString(callback).build().getQueryParams().getFirst("state"));

        when(revoked.existsById(access)).thenReturn(true);
        mvc.perform(authorize(access)).andExpect(status().isUnauthorized());
    }

    @Test
    void exchangeBindsPkceAndSecretAndIntrospectionReadsLiveRolesAndLogout() throws Exception {
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account(83, "ROLE_ADMIN")));
        String code = code();
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> sso.exchange("wrong-secret", code, STATE, VERIFIER)).getRawStatusCode());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.exchange(SECRET, code, STATE, "w".repeat(43))).getRawStatusCode());

        var result = sso.exchange(SECRET, code, STATE, VERIFIER);
        assertEquals(List.of("ROLE_ADMIN"), result.get("authorities"));
        assertTrue(Instant.ofEpochMilli((Long) result.get("expiresAt")).isAfter(Instant.now().plusSeconds(28790)));
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.exchange(SECRET, code, STATE, VERIFIER)).getRawStatusCode());

        String grant = (String) result.get("grant");
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account(83, "ROLE_KUMOH")));
        assertEquals(List.of("ROLE_KUMOH"), sso.identity(SECRET, grant).get("authorities"));
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.empty());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.identity(SECRET, grant)).getRawStatusCode());

        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account(83, "ROLE_KUMOH")));
        String pending = code();
        sso.revokeAccount(83);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.identity(SECRET, grant)).getRawStatusCode());
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.exchange(SECRET, pending, STATE, VERIFIER)).getRawStatusCode());
    }

    @Test
    void refreshResolvesKakaoSubjectToTheCurrentBoardAccount() {
        var account = OAuthAccount.builder().accountId(83L).loginId("student@example.test")
                .status(Status.NORMAL).provider("kakao").sub("kakao-subject")
                .roleAccounts(List.of(new RoleAccount(null, Role.builder().name("ROLE_KUMOH").build())))
                .build();
        when(oauth.findOAuthAccountBySubAndProvider("kakao-subject", "kakao")).thenReturn(Optional.of(account));
        when(accounts.findByLoginIdWithRole("student@example.test")).thenReturn(Optional.of(account));
        var authentication = new UsernamePasswordAuthenticationToken("kakao-subject", "", List.of());
        var previous = provider.createLargeRefreshToken(authentication).getToken();

        var refreshed = tokens.refresh(previous);
        var claims = decoder.getClaims(new com.seproject.account.token.domain.JWT(refreshed.getAccessToken()));
        assertEquals("student@example.test", claims.getSubject());
        assertEquals(83L, ((Number) claims.get("accountId")).longValue());
        assertNotEquals(previous, refreshed.getRefreshToken());
        assertNotEquals(provider.createRefreshToken(authentication).getToken(),
                provider.createRefreshToken(authentication).getToken());
    }

    @Test
    void missingGenerationCannotRestoreAnOldGrant() throws Exception {
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account(83, "ROLE_USER")));
        String grant = (String) sso.exchange(SECRET, code(), STATE, VERIFIER).get("grant");
        records.remove("allday:sso:generation:83");
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> sso.identity(SECRET, grant)).getRawStatusCode());
        sso.exchange(SECRET, code(), STATE, VERIFIER);
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> sso.identity(SECRET, grant)).getRawStatusCode());
    }

    @Test
    void parallelRedemptionOnlyCreatesOneGrant() throws Exception {
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account(83, "ROLE_USER")));
        String code = code();
        var executor = Executors.newFixedThreadPool(4);
        try {
            var results = executor.<Integer>invokeAll(java.util.Collections.nCopies(4, () -> {
                try { sso.exchange(SECRET, code, STATE, VERIFIER); return 200; }
                catch (ResponseStatusException e) { return e.getRawStatusCode(); }
            }));
            int successes = 0;
            for (Future<Integer> result : results) if (result.get() == 200) successes++;
            assertEquals(1, successes);
        } finally {
            executor.shutdownNow();
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authorize(String token) throws Exception {
        return post(AUTHORIZE).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(input());
    }

    private String input() throws Exception {
        return json.writeValueAsString(Map.of("state", STATE, "codeChallenge", challenge()));
    }

    private String code() throws Exception {
        var url = sso.issue(83, Instant.now().plusSeconds(120), STATE, challenge(), "verified-test-access");
        return UriComponentsBuilder.fromUri(url).build().getQueryParams().getFirst("code");
    }

    private String challenge() throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
    }

    private FormAccount account(long id, String roleName) {
        Role role = Role.builder().name(roleName).build();
        return FormAccount.builder().accountId(id).loginId("student").status(Status.NORMAL)
                .roleAccounts(List.of(new RoleAccount(null, role))).build();
    }
}
