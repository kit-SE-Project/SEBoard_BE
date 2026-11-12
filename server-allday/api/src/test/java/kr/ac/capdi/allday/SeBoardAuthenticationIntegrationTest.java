package kr.ac.capdi.allday;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "allday.dev-auth-enabled=false", "allday.dev-seed-enabled=false", "allday.scheduler-enabled=false",
    "allday.seboard-sso.portal-url=http://127.0.0.1:35672/allday/sso",
    "allday.seboard-sso.public-origin=http://127.0.0.1:35671", "allday.cors-origin=http://127.0.0.1:35671",
    "allday.seboard-sso.client-secret=private-test-sso-secret-not-for-deployment"
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".*[/]capdi_test")
class SeBoardAuthenticationIntegrationTest {
    private static final String SECRET = "private-test-sso-secret-not-for-deployment";
    private static final String PUBLIC = "http://127.0.0.1:35671";
    private static final String CODE = "c".repeat(43);
    private static final String GRANT = "g".repeat(43);
    private static final long ACCOUNT = 739302;
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicReference<Flow> FLOW = new AtomicReference<>();
    private static final AtomicReference<String> ROLE = new AtomicReference<>();
    private static final AtomicInteger IDENTITY_STATUS = new AtomicInteger();
    private static final AtomicInteger EXCHANGES = new AtomicInteger();
    private static final HttpServer BOARD = board();

    @Autowired JdbcTemplate db;
    @LocalServerPort int port;
    private final HttpClient browser = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @DynamicPropertySource
    static void identityEndpoint(DynamicPropertyRegistry registry) {
        registry.add("allday.seboard-sso.backend-origin", () -> "http://127.0.0.1:" + BOARD.getAddress().getPort());
    }

    @AfterAll
    static void stopBoard() {
        BOARD.stop(0);
    }

    @BeforeEach
    void reset() {
        IDENTITY_STATUS.set(200);
        ROLE.set("ROLE_ADMIN");
        EXCHANGES.set(0);
        db.update("DELETE FROM allday_sso_login");
        db.update("DELETE FROM allday_sso_session");
    }

    @Test
    void exchangeCreatesOnlyRentalCookieAndReadsRolesOnEveryRequest() throws Exception {
        var flow = start();
        var unbound = request("GET", callback(flow.state()), null, null);
        assertThat(location(unbound)).isEqualTo(PUBLIC + "/?sso=expired");
        assertThat(EXCHANGES.get()).isZero();

        var response = request("GET", callback(flow.state()), flow.cookie(), null);
        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(location(response)).isEqualTo(PUBLIC + "/");
        assertThat(response.headers().firstValue("Referrer-Policy")).contains("no-referrer");
        var cookie = session(response);
        assertThat(cookie).doesNotContain(GRANT).doesNotContain(CODE);
        assertThat(EXCHANGES.get()).isOne();
        assertThat(location(request("GET", callback(flow.state()), flow.cookie(), null))).endsWith("sso=expired");
        assertThat(EXCHANGES.get()).isOne();

        var me = request("GET", "/api/v1/me", cookie, null);
        assertThat(JSON.readTree(me.body()).path("memberId").asLong()).isEqualTo(ACCOUNT);
        assertThat(JSON.readTree(me.body()).path("role").asText()).isEqualTo("ADMIN");
        assertThat(request("GET", "/api/v1/admin/categories", cookie, null).statusCode()).isEqualTo(200);
        assertThat(JSON.readTree(request("GET", "/api/v1/sso/profile", cookie, null).body()).path("nickname").asText())
            .isEqualTo("시험 회원");
        assertThat(db.queryForObject("SELECT issuer FROM external_subject_binding WHERE member_id=?", String.class, ACCOUNT))
            .isEqualTo("seboard");

        ROLE.set("ROLE_USER");
        assertThat(request("GET", "/api/v1/admin/categories", cookie, null).statusCode()).isEqualTo(403);
        assertThat(JSON.readTree(request("GET", "/api/v1/me", cookie, null).body()).path("rentalEligible").asBoolean()).isFalse();
        assertThat(request("GET", "/api/internal/inventory", cookie, null).statusCode()).isEqualTo(401);
        assertThat(request("GET", "/api/v1/me", null, null).statusCode()).isEqualTo(401);
        var direct = HttpRequest.newBuilder(url("/api/v1/me"))
            .header("Authorization", "Bearer unused.board.token").GET().build();
        assertThat(browser.send(direct, HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);

        IDENTITY_STATUS.set(503);
        assertThat(request("GET", "/api/v1/me", cookie, null).statusCode()).isEqualTo(503);
        IDENTITY_STATUS.set(401);
        assertThat(request("GET", "/api/v1/me", cookie, null).statusCode()).isEqualTo(401);
        IDENTITY_STATUS.set(200);
        assertThat(request("GET", "/api/v1/me", cookie, null).statusCode()).isEqualTo(401);
    }

    @Test
    void logoutAndMutationsRequireOwnOriginAndLogoutDeletesSession() throws Exception {
        var flow = start();
        String cookie = session(request("GET", callback(flow.state()), flow.cookie(), null));
        assertThat(request("POST", "/api/v1/sso/logout", cookie, "https://other.example").statusCode()).isEqualTo(403);
        assertThat(request("POST", "/api/v1/admin/categories", cookie, null).statusCode()).isEqualTo(403);
        assertThat(request("GET", "/api/v1/me", cookie, null).statusCode()).isEqualTo(200);
        var logout = request("POST", "/api/v1/sso/logout", cookie, PUBLIC);
        assertThat(logout.statusCode()).isEqualTo(204);
        assertThat(logout.headers().firstValue("Set-Cookie").orElseThrow()).contains("Max-Age=0");
        assertThat(request("GET", "/api/v1/me", cookie, null).statusCode()).isEqualTo(401);
    }

    @Test
    void expiredAndConcurrentCallbacksCannotReplay() throws Exception {
        var expired = start();
        db.update("UPDATE allday_sso_login SET expires_at=now()-interval '1 minute'");
        assertThat(location(request("GET", callback(expired.state()), expired.cookie(), null))).endsWith("sso=expired");
        assertThat(EXCHANGES.get()).isZero();
        var flow = start();
        var callback = HttpRequest.newBuilder(url(callback(flow.state()))).header("Cookie", flow.cookie()).GET().build();
        var first = browser.sendAsync(callback, HttpResponse.BodyHandlers.ofString());
        var second = browser.sendAsync(callback, HttpResponse.BodyHandlers.ofString());
        assertThat(List.of(location(first.get()), location(second.get())))
            .containsExactlyInAnyOrder(PUBLIC + "/", PUBLIC + "/?sso=expired");
        assertThat(EXCHANGES.get()).isOne();
    }

    private Flow start() throws Exception {
        var response = request("GET", "/api/v1/sso/start", null, null);
        assertThat(response.statusCode()).isEqualTo(303);
        URI portal = URI.create(location(response));
        assertThat(portal.getPath()).isEqualTo("/allday/sso");
        assertThat(portal.getQuery()).isNull();
        var args = UriComponentsBuilder.fromPath("/").query(portal.getFragment()).build().getQueryParams();
        String flowCookie = response.headers().firstValue("Set-Cookie").orElseThrow();
        assertThat(flowCookie).contains("HttpOnly", "SameSite=Lax");
        var flow = new Flow(args.getFirst("state"), args.getFirst("code_challenge"), flowCookie.split(";", 2)[0]);
        FLOW.set(flow);
        return flow;
    }

    private HttpResponse<String> request(String method, String path, String cookie, String origin) throws Exception {
        var request = HttpRequest.newBuilder(url(path)).method(method, HttpRequest.BodyPublishers.noBody());
        if (cookie != null) request.header("Cookie", cookie);
        if (origin != null) request.header("Origin", origin);
        return browser.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI url(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private static String callback(String state) {
        return "/api/v1/sso/callback?code=" + CODE + "&state=" + state;
    }

    private static String location(HttpResponse<String> response) {
        return response.headers().firstValue("Location").orElseThrow();
    }

    private static String session(HttpResponse<String> response) {
        var header = response.headers().allValues("Set-Cookie").stream()
            .filter(cookie -> cookie.startsWith("allday_session=")).findFirst().orElseThrow();
        assertThat(header).contains("HttpOnly", "SameSite=Lax", "Path=/");
        return header.split(";", 2)[0];
    }

    private static HttpServer board() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/internal/allday/sso/", request -> {
                try {
                    var body = JSON.readTree(request.getRequestBody());
                    int status = 403;
                    Map<String, ?> result = Map.of();
                    if ("POST".equals(request.getRequestMethod())
                            && SECRET.equals(request.getRequestHeaders().getFirst("X-AllDay-Client-Secret"))
                            && request.getRequestHeaders().getFirst("Authorization") == null) {
                        if (request.getRequestURI().getPath().endsWith("/exchange")) {
                            var flow = FLOW.get();
                            String verifier = body.path("codeVerifier").asText();
                            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
                            String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                            if (CODE.equals(body.path("code").asText()) && flow.state().equals(body.path("state").asText())
                                    && flow.challenge().equals(challenge)) {
                                EXCHANGES.incrementAndGet();
                                status = 200;
                                result = Map.of("grant", GRANT, "expiresAt", Instant.now().plusSeconds(600).toEpochMilli(),
                                    "accountId", ACCOUNT, "nickname", "시험 회원", "authorities", List.of(ROLE.get()));
                            }
                        } else if (GRANT.equals(body.path("grant").asText())) {
                            status = IDENTITY_STATUS.get();
                            result = Map.of("accountId", ACCOUNT, "nickname", "시험 회원", "authorities", List.of(ROLE.get()));
                        }
                    }
                    byte[] bytes = JSON.writeValueAsBytes(result);
                    request.sendResponseHeaders(status, bytes.length);
                    try (var output = request.getResponseBody()) { output.write(bytes); }
                } catch (Exception e) {
                    request.sendResponseHeaders(500, -1);
                    request.close();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Flow(String state, String challenge, String cookie) {}
}
