package com.seproject.allday;

import com.seproject.account.account.domain.FormAccount;
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
import com.seproject.error.exception.CustomAuthenticationException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.RequestContextFilter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AlldayIdentityControllerTest {
    private static final String PATH = "/internal/allday/identity";
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final OAuthAccountRepository oauth = mock(OAuthAccountRepository.class);
    private final LogoutTokenRepository revoked = mock(LogoutTokenRepository.class);
    private final JwtProvider provider = new JwtProvider();
    private MockMvc mvc;
    private TokenService tokens;

    @BeforeEach
    void setup() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        JwtDecoder decoder = new JwtDecoder(accounts);
        ReflectionTestUtils.setField(provider, "secret", secret);
        ReflectionTestUtils.setField(decoder, "secret", secret);
        tokens = new TokenService(accounts, oauth, provider, decoder,
                mock(LogoutRefreshTokenRepository.class), mock(LogoutLargeRefreshTokenRepository.class));
        var failures = new CustomAuthenticationFailureHandler(new ResponseWriter(), new ErrorCodeConverter());
        mvc = MockMvcBuilders.standaloneSetup(new AlldayIdentityController(decoder))
                .addFilters(new RequestContextFilter(), new SecurityContextPersistenceFilter(),
                        new JwtFilter(tokens, failures, revoked))
                .build();
    }

    @Test
    void rolesComeFromTheCurrentAccountAndDeletionOrRevocationTakesEffect() throws Exception {
        var admin = account(83, "ROLE_ADMIN");
        var authentication = new UsernamePasswordAuthenticationToken(admin, "", admin.getAuthorities());
        String token = provider.createAccessToken(authentication).getToken();
        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.of(admin));
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.accountId").value(83)).andExpect(jsonPath("$.authorities[0]").value("ROLE_ADMIN"));

        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.of(account(83, "ROLE_KUMOH")));
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.authorities[0]").value("ROLE_KUMOH"));

        when(revoked.existsById(token)).thenReturn(true);
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        when(revoked.existsById(token)).thenReturn(false);
        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.empty());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRefreshExpiredForgedAndReassignedLoginTokens() throws Exception {
        var account = account(83, "ROLE_USER");
        var authentication = new UsernamePasswordAuthenticationToken(account, "", account.getAuthorities());
        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.of(account));
        String access = provider.createAccessToken(authentication).getToken();
        String refresh = provider.createRefreshToken(authentication).getToken();
        String expired = provider.createToken(authentication, JWTProperties.ACCESS_TOKEN, JWTProperties.HS256,
                Date.from(Instant.now().minusSeconds(60)), Date.from(Instant.now().minusSeconds(30))).getToken();

        assertThrows(CustomAuthenticationException.class, () -> tokens.refresh(access));
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + refresh)).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + access + "bad")).andExpect(status().isUnauthorized());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + expired))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(108));

        when(accounts.findByLoginIdWithRole("student")).thenReturn(Optional.of(account(84, "ROLE_USER")));
        mvc.perform(get(PATH).header("Authorization", "Bearer " + access)).andExpect(status().isUnauthorized());
    }

    private FormAccount account(long id, String roleName) {
        Role role = Role.builder().name(roleName).build();
        return FormAccount.builder().accountId(id).loginId("student").status(Status.NORMAL)
                .roleAccounts(List.of(new RoleAccount(null, role))).build();
    }
}
