package kr.ac.capdi.allday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SeBoardAuthentication {
    static final Duration FLOW_TTL = Duration.ofMinutes(5);
    private static final String ISSUER = "seboard";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final URI backend;
    private final URI portal;
    private final URI publicOrigin;
    private final String clientSecret;
    private final String rentalAuthority;
    private final ObjectMapper json;
    private final JdbcTemplate db;
    private final Clock clock;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();

    public SeBoardAuthentication(@Value("${allday.seboard-sso.backend-origin:}") String backendUrl,
            @Value("${allday.seboard-sso.portal-url:}") String portalUrl,
            @Value("${allday.seboard-sso.public-origin:}") String publicUrl,
            @Value("${allday.seboard-sso.client-secret:}") String secret,
            @Value("${allday.seboard-rental-authority:ROLE_KUMOH}") String rentalAuthority,
            ObjectMapper json, JdbcTemplate db, Clock clock) {
        if (backendUrl.isBlank() && portalUrl.isBlank() && publicUrl.isBlank() && secret.isEmpty()) {
            backend = portal = publicOrigin = null;
        } else {
            backend = configuredUri(backendUrl, "");
            portal = configuredUri(portalUrl, "/allday/sso");
            publicOrigin = configuredUri(publicUrl, "");
            if (secret.getBytes(StandardCharsets.UTF_8).length < 32 || rentalAuthority.isBlank()
                    || !rentalAuthority.equals(rentalAuthority.strip())) {
                throw new IllegalArgumentException("Configure the AllDay SSO client secret and rental authority.");
            }
        }
        this.clientSecret = secret;
        this.rentalAuthority = rentalAuthority;
        this.json = json;
        this.db = db;
        this.clock = clock;
    }

    record Login(URI location, String browserNonce) {}
    record Session(String token, Duration lifetime) {}
    record Identity(Actor actor, String nickname) {}

    public boolean enabled() {
        return backend != null;
    }

    URI publicLocation(String error) {
        var uri = UriComponentsBuilder.fromUri(publicOrigin).replacePath("/");
        if (error != null) uri.queryParam("sso", error);
        return uri.build().toUri();
    }

    boolean secureCookies() {
        return "https".equals(publicOrigin.getScheme());
    }

    void requireOrigin(String origin) {
        requireEnabled();
        if (!publicOrigin.toASCIIString().equals(origin)) {
            throw new ApiProblem(403, "SSO_ORIGIN_MISMATCH", "대여 사이트에서 다시 요청하세요.");
        }
    }

    @Transactional
    public Login begin(String previousNonce) {
        requireEnabled();
        var now = clock.instant();
        String state = random(), nonce = random(), verifier = random();
        db.update("DELETE FROM allday_sso_login WHERE expires_at<=? OR browser_hash=?",
            Rows.ts(now), valid(previousNonce) ? hash(previousNonce) : null);
        db.update("INSERT INTO allday_sso_login(state_hash,browser_hash,verifier,expires_at) VALUES (?,?,?,?)",
            hash(state), hash(nonce), verifier, Rows.ts(now.plus(FLOW_TTL)));
        var location = UriComponentsBuilder.fromUri(portal)
            .fragment("state=" + state + "&code_challenge=" + hash(verifier)).build().toUri();
        return new Login(location, nonce);
    }

    public Session finish(String code, String state, String nonce, String previousSession) {
        requireEnabled();
        if (!valid(code) || !valid(state) || !valid(nonce)) throw unauthorized();
        var attempt = db.queryForList("""
            DELETE FROM allday_sso_login WHERE state_hash=? AND browser_hash=? AND expires_at>?
            RETURNING verifier
            """, hash(state), hash(nonce), Rows.ts(clock.instant()));
        if (attempt.isEmpty()) throw unauthorized();
        JsonNode exchanged = call("exchange", Map.of("code", code, "state", state,
            "codeVerifier", attempt.getFirst().get("verifier")));
        var identity = readIdentity(exchanged);
        String grant = exchanged.path("grant").asText("");
        JsonNode exp = exchanged.path("expiresAt");
        if (!valid(grant) || !exp.isIntegralNumber() || !exp.canConvertToLong()) throw unavailable();

        Instant now = clock.instant();
        Instant expires = Instant.ofEpochMilli(exp.asLong());
        Duration lifetime = Duration.between(now, expires);
        if (lifetime.toSeconds() < 1) throw unauthorized();
        if (lifetime.compareTo(Duration.ofMinutes(30)) > 0) throw unavailable();
        bind(identity.actor().memberId(), identity.actor().rentalEligible());
        String session = random();
        db.update("DELETE FROM allday_sso_session WHERE expires_at<=?", Rows.ts(now));
        db.update("INSERT INTO allday_sso_session(token_hash,member_id,board_grant,expires_at) VALUES (?,?,?,?)",
            hash(session), identity.actor().memberId(), grant, Rows.ts(expires));
        close(previousSession);
        return new Session(session, lifetime);
    }

    Identity authenticate(String token) {
        if (!valid(token)) throw unauthorized();
        String tokenHash = hash(token);
        var sessions = db.queryForList("SELECT member_id,board_grant FROM allday_sso_session WHERE token_hash=? AND expires_at>?",
            tokenHash, Rows.ts(clock.instant()));
        if (sessions.isEmpty()) throw unauthorized();
        var session = sessions.getFirst();
        try {
            var identity = readIdentity(call("identity", Map.of("grant", session.get("board_grant"))));
            if (identity.actor().memberId() != Rows.number(session, "member_id")) throw unauthorized();
            return identity;
        } catch (ApiProblem e) {
            if (e.status == 401) close(token);
            throw e;
        }
    }

    void close(String token) {
        if (valid(token)) db.update("DELETE FROM allday_sso_session WHERE token_hash=?", hash(token));
    }

    void requireEnabled() {
        if (!enabled()) throw new ApiProblem(503, "SSO_NOT_CONFIGURED", "SE 게시판 로그인 연결을 설정해야 합니다.");
    }

    private Identity readIdentity(JsonNode body) {
        var id = body.path("accountId");
        var roles = body.path("authorities");
        var nickname = body.path("nickname");
        if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0
                || !roles.isArray() || !nickname.isTextual()) throw unavailable();
        var authorities = new HashSet<String>();
        for (var role : roles) {
            if (!role.isTextual() || role.asText().isBlank()) throw unavailable();
            authorities.add(role.asText());
        }
        boolean admin = authorities.contains("ROLE_ADMIN");
        return new Identity(new Actor(id.asLong(), admin ? "ADMIN" : "STUDENT", authorities,
            admin || authorities.contains(rentalAuthority)), nickname.asText());
    }

    private JsonNode call(String endpoint, Map<String, ?> payload) {
        try {
            var request = HttpRequest.newBuilder(backend.resolve("/internal/allday/sso/" + endpoint))
                .timeout(Duration.ofSeconds(4)).header("X-AllDay-Client-Secret", clientSecret)
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 401) throw unauthorized();
            if (response.statusCode() != 200) throw unavailable();
            var body = json.readTree(response.body());
            if (body == null || !body.isObject()) throw unavailable();
            return body;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException e) {
            throw unavailable();
        }
    }

    private void bind(long accountId, boolean eligible) {
        String subject = Long.toString(accountId);
        db.update("""
            INSERT INTO external_subject_binding(member_id,issuer,subject,last_eligibility,last_checked_at)
            VALUES (?,?,?,?,now()) ON CONFLICT DO NOTHING
            """, accountId, ISSUER, subject, eligible);
        var binding = db.queryForList("SELECT issuer,subject FROM external_subject_binding WHERE member_id=?", accountId);
        if (binding.size() != 1 || !ISSUER.equals(binding.getFirst().get("issuer"))
                || !subject.equals(binding.getFirst().get("subject"))) {
            throw new ApiProblem(503, "SEBOARD_IDENTITY_CONFLICT", "기존 대여 계정과 SEBoard 계정의 연결을 확인하세요.");
        }
    }

    private static URI configuredUri(String value, String path) {
        URI uri = URI.create(value);
        boolean secure = "https".equals(uri.getScheme());
        boolean local = "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost());
        if ((!secure && !local) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null
                || !(path.equals(uri.getRawPath()) || (path.isEmpty() && "/".equals(uri.getRawPath())))) {
            throw new IllegalArgumentException("Configure fixed HTTPS URLs for AllDay SSO.");
        }
        if (!path.isEmpty()) return uri;
        int port = uri.getPort();
        if (port == 0 || port > 65535) throw new IllegalArgumentException("Invalid SSO origin port.");
        boolean defaultPort = port == (secure ? 443 : 80);
        return URI.create(uri.getScheme() + "://" + uri.getHost().toLowerCase(Locale.ROOT)
            + (port < 0 || defaultPort ? "" : ":" + port));
    }

    private static String random() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean valid(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ApiProblem unauthorized() {
        return new ApiProblem(401, "UNAUTHENTICATED", "SE 게시판에서 다시 로그인하세요.");
    }

    private static ApiProblem unavailable() {
        return new ApiProblem(503, "SEBOARD_AUTH_UNAVAILABLE", "SE 게시판 인증을 확인할 수 없습니다.");
    }
}
