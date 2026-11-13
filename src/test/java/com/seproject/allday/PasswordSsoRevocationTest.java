package com.seproject.allday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seproject.account.account.application.AccountAppService;
import com.seproject.account.account.controller.dto.PasswordDTO.PasswordChangeRequest;
import com.seproject.account.account.controller.dto.PasswordDTO.ResetPasswordRequest;
import com.seproject.account.account.domain.FormAccount;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.account.service.AccountService;
import com.seproject.account.email.service.PasswordChangeEmailService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.account.token.domain.repository.LogoutTokenRepository;
import com.seproject.board.common.Status;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.member.domain.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real SSO revocation and Spring transaction interception; isolated H2 and mocked Redis. */
class PasswordSsoRevocationTest {
    private static final String SECRET = "password-revocation-test-secret-at-least-32-bytes";
    private static final String EMAIL = "password-fixture@example.test";
    private static final String OLD = "test-only-old-password";
    private static final String NEXT = "test-only-new-password";
    private static final String STATE = "s".repeat(43);
    private static final String VERIFIER = "v".repeat(43);
    private final Map<String, String> records = new ConcurrentHashMap<>();
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final AccountService accountService = mock(AccountService.class);
    private final PasswordChangeEmailService emailService = mock(PasswordChangeEmailService.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private ValueOperations<String, String> values;
    private AlldaySsoService sso;
    private AccountAppService app;
    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;
    private FormAccount account;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        jdbc = new JdbcTemplate(database);
        jdbc.execute("create table password_fixture (id bigint primary key, password_value varchar(255))");
        jdbc.update("insert into password_fixture values (?, ?)", 83L, OLD);
        account = account(83);
        when(accounts.findActiveByIdWithRole(83)).thenReturn(Optional.of(account));
        when(accounts.findActiveByIdWithRole(84)).thenReturn(Optional.of(account(84)));
        when(accountService.findById(83L)).thenReturn(account);
        when(emailService.isConfirmed(EMAIL)).thenReturn(true);
        when(accountService.matchPassword(account, OLD)).thenReturn(true);
        when(accountService.resetPassword(EMAIL, NEXT)).thenAnswer(args -> { writePassword(); return 83L; });
        doAnswer(args -> { writePassword(); return null; }).when(accountService).changePassword(account, NEXT);

        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenAnswer(args -> records.get(args.getArgument(0)));
        when(values.getAndDelete(anyString())).thenAnswer(args -> records.remove(args.getArgument(0)));
        when(values.setIfAbsent(anyString(), anyString())).thenAnswer(args ->
                records.putIfAbsent(args.getArgument(0), args.getArgument(1)) == null);
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(args ->
                records.putIfAbsent(args.getArgument(0), args.getArgument(1)) == null);
        when(values.setIfPresent(anyString(), anyString())).thenAnswer(args ->
                records.replace(args.getArgument(0), args.getArgument(1)) != null);
        doAnswer(args -> records.put(args.getArgument(0), args.getArgument(1))).when(values)
                .set(anyString(), anyString(), any(Duration.class));
        sso = sso(true);
        app = transactionalApp();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(account, "", account.getAuthorities()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        database.shutdown();
    }

    @Test
    void resetRevokesAllExistingGrantsAndPendingCodesForOnlyThatAccount() throws Exception {
        String first = grant(83);
        String second = grant(83);
        String otherAccount = grant(84);
        String pending = code(83);
        assertEquals(EMAIL, app.resetPassword(resetRequest()).getEmail());
        assertEquals(NEXT, password());
        assertRevoked(first);
        assertRevoked(second);
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> exchange(pending)).getRawStatusCode());
        assertEquals(84L, sso.identity(SECRET, otherAccount).get("accountId"));
        assertEquals(83L, sso.identity(SECRET, grant(83)).get("accountId"));
    }

    @Test
    void authenticatedPasswordChangeAlsoRevokesExistingRentalLogin() throws Exception {
        String existing = grant(83);
        app.changePassword(changeRequest());
        assertEquals(NEXT, password());
        assertRevoked(existing);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectedProofDoesNotChangePasswordOrRevokeLogin(boolean change) throws Exception {
        String existing = grant(83);
        if (change) when(accountService.matchPassword(account, OLD)).thenReturn(false);
        else when(emailService.isConfirmed(EMAIL)).thenReturn(false);
        assertThrows(CustomIllegalArgumentException.class, () -> updatePassword(change));
        assertEquals(OLD, password());
        assertEquals(83L, sso.identity(SECRET, existing).get("accountId"));
        verify(values, never()).setIfPresent(anyString(), anyString());
    }

    @Test
    void failedPasswordWriteRollsBackAndDoesNotRevokeLogin() throws Exception {
        String existing = grant(83);
        doAnswer(args -> {
            writePassword();
            throw new IllegalStateException("test-only password persistence failure");
        }).when(accountService).resetPassword(EMAIL, NEXT);
        assertThrows(IllegalStateException.class, () -> app.resetPassword(resetRequest()));
        assertEquals(OLD, password());
        assertEquals(83L, sso.identity(SECRET, existing).get("accountId"));
        verify(values, never()).setIfPresent(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void revocationFailureRollsBackPasswordRatherThanReturningSuccess(boolean change) throws Exception {
        String existing = grant(83);
        when(values.setIfPresent(anyString(), anyString())).thenThrow(
                new RedisConnectionFailureException("test-only revocation outage"));
        assertThrows(RedisConnectionFailureException.class, () -> updatePassword(change));
        assertEquals(OLD, password());
        assertEquals(83L, sso.identity(SECRET, existing).get("accountId"));
    }

    @Test
    void disabledSsoKeepsExistingPasswordResetAvailableWithoutRedisRevocation() {
        sso = sso(false);
        app = transactionalApp();
        assertEquals(EMAIL, app.resetPassword(resetRequest()).getEmail());
        assertEquals(NEXT, password());
        verify(values, never()).setIfPresent(anyString(), anyString());
    }

    private AccountAppService transactionalApp() {
        var target = new AccountAppService(null, accountService, null, null, sso, emailService, null, null, null, null);
        var proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(database),
                new AnnotationTransactionAttributeSource()));
        return (AccountAppService) proxy.getProxy();
    }

    private AlldaySsoService sso(boolean enabled) {
        return new AlldaySsoService(enabled ? "https://rental.example/api/v1/sso/callback" : "",
                enabled ? SECRET : "", redis, new ObjectMapper(), accounts,
                mock(MemberRepository.class), mock(LogoutTokenRepository.class));
    }

    private void writePassword() {
        assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
        jdbc.update("update password_fixture set password_value = ? where id = ?", NEXT, 83L);
    }

    private String password() {
        return jdbc.queryForObject("select password_value from password_fixture where id = 83", String.class);
    }

    private FormAccount account(long id) {
        return FormAccount.builder().accountId(id).loginId(EMAIL).status(Status.NORMAL)
                .roleAccounts(List.of(new RoleAccount(null, Role.builder().name("ROLE_USER").build()))).build();
    }

    private ResetPasswordRequest resetRequest() {
        var request = new ResetPasswordRequest(); request.setEmail(EMAIL); request.setPassword(NEXT); return request;
    }

    private PasswordChangeRequest changeRequest() {
        var request = new PasswordChangeRequest(); request.setNowPassword(OLD); request.setNewPassword(NEXT); return request;
    }

    private void updatePassword(boolean change) {
        if (change) app.changePassword(changeRequest()); else app.resetPassword(resetRequest());
    }

    private String code(long id) throws Exception {
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                .digest(VERIFIER.getBytes(StandardCharsets.US_ASCII)));
        return UriComponentsBuilder.fromUri(sso.issue(id, Instant.now().plusSeconds(120), STATE, challenge, "test-access"))
                .build().getQueryParams().getFirst("code");
    }

    private Map<String, Object> exchange(String code) {
        return sso.exchange(SECRET, code, STATE, VERIFIER);
    }

    private String grant(long id) throws Exception { return (String) exchange(code(id)).get("grant"); }

    private void assertRevoked(String grant) {
        assertEquals(401, assertThrows(ResponseStatusException.class,
                () -> sso.identity(SECRET, grant)).getRawStatusCode());
    }
}
