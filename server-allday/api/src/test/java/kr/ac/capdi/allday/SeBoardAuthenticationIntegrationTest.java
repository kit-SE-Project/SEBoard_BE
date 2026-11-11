package kr.ac.capdi.allday;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "allday.dev-auth-enabled=false", "allday.dev-seed-enabled=false", "allday.scheduler-enabled=false"
})
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".*[/]capdi_test")
class SeBoardAuthenticationIntegrationTest {
    private static final String TOKEN = "board.header.signature";
    private static final String PATH = "/internal/allday/identity";
    private static final long ACCOUNT = 739302;
    private static final AtomicReference<Reply> IDENTITY = new AtomicReference<>();
    private static final HttpServer BOARD = board();

    @Autowired TestRestTemplate http;
    @Autowired JdbcTemplate db;

    @DynamicPropertySource
    static void identityEndpoint(DynamicPropertyRegistry registry) {
        registry.add("allday.seboard-identity-url", () -> "http://127.0.0.1:" + BOARD.getAddress().getPort() + PATH);
    }

    @AfterAll
    static void stopBoard() {
        BOARD.stop(0);
    }

    @Test
    void readsCurrentRolesAndEnforcesAdminAndWorkerBoundaries() {
        IDENTITY.set(identity("ROLE_ADMIN"));
        assertThat(get("/api/v1/me", TOKEN).getBody())
            .containsEntry("memberId", (int) ACCOUNT).containsEntry("role", "ADMIN").containsEntry("rentalEligible", true);
        var headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        assertThat(http.exchange("/api/v1/admin/categories", HttpMethod.GET, new HttpEntity<>(headers), String.class)
            .getStatusCode().value()).isEqualTo(200);

        IDENTITY.set(identity("ROLE_USER"));
        assertThat(get("/api/v1/me", TOKEN).getBody()).containsEntry("rentalEligible", false);
        assertThat(get("/api/v1/admin/categories", TOKEN).getStatusCode().value()).isEqualTo(403);
        assertThat(get("/api/internal/inventory", TOKEN).getStatusCode().value()).isEqualTo(401);
        assertThat(get("/api/v1/me", System.getenv("WORKER_TOKEN")).getStatusCode().value()).isEqualTo(401);
        assertThat(db.queryForObject("SELECT issuer FROM external_subject_binding WHERE member_id=?", String.class, ACCOUNT))
            .isEqualTo("seboard");
    }

    @Test
    void rejectsRevokedExpiredAndUnavailableIdentity() {
        IDENTITY.set(new Reply(401, "{}"));
        assertThat(get("/api/v1/me", TOKEN).getStatusCode().value()).isEqualTo(401);
        IDENTITY.set(new Reply(404, "{\"code\":108}"));
        assertThat(get("/api/v1/me", TOKEN).getBody()).containsEntry("code", "TOKEN_EXPIRED");
        IDENTITY.set(new Reply(503, "{}"));
        assertThat(get("/api/v1/me", TOKEN).getStatusCode().value()).isEqualTo(503);
    }

    @Test
    void cannotInheritAnExistingDemoIdentity() {
        long otherAccount = ACCOUNT + 1;
        db.update("INSERT INTO external_subject_binding(member_id,issuer,subject) VALUES (?,'development','fixture')",
            otherAccount);
        try {
            IDENTITY.set(new Reply(200, "{\"accountId\":" + otherAccount + ",\"authorities\":[\"ROLE_ADMIN\"]}"));
            assertThat(get("/api/v1/me", TOKEN).getBody()).containsEntry("code", "SEBOARD_IDENTITY_CONFLICT");
        } finally {
            db.update("DELETE FROM external_subject_binding WHERE member_id=?", otherAccount);
        }
    }

    private ResponseEntity<Map> get(String path, String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private static Reply identity(String role) {
        return new Reply(200, "{\"accountId\":" + ACCOUNT + ",\"authorities\":[\"" + role + "\"]}");
    }

    private static HttpServer board() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext(PATH, request -> {
                Reply reply = ("Bearer " + TOKEN).equals(request.getRequestHeaders().getFirst("Authorization"))
                    ? IDENTITY.get() : new Reply(401, "{}");
                byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
                request.sendResponseHeaders(reply.status(), body.length);
                try (var output = request.getResponseBody()) {
                    output.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private record Reply(int status, String body) {}
}
