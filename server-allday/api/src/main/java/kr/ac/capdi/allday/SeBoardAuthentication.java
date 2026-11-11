package kr.ac.capdi.allday;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SeBoardAuthentication {
    private static final String ISSUER = "seboard";
    private final URI endpoint;
    private final String rentalAuthority;
    private final ObjectMapper json;
    private final JdbcTemplate db;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build();

    public SeBoardAuthentication(@Value("${allday.seboard-identity-url:}") String url,
            @Value("${allday.seboard-rental-authority:ROLE_KUMOH}") String rentalAuthority,
            ObjectMapper json, JdbcTemplate db) {
        endpoint = url.isBlank() ? null : URI.create(url);
        if (endpoint != null) {
            boolean secure = "https".equals(endpoint.getScheme());
            boolean loopback = "http".equals(endpoint.getScheme()) &&
                ("127.0.0.1".equals(endpoint.getHost()) || "[::1]".equals(endpoint.getHost()));
            if ((!secure && !loopback) || endpoint.getHost() == null || endpoint.getUserInfo() != null
                    || endpoint.getQuery() != null || endpoint.getFragment() != null
                    || !"/internal/allday/identity".equals(endpoint.getPath())) {
                throw new IllegalArgumentException("SEBOARD_IDENTITY_URL must point to the SEBoard identity endpoint over HTTPS or loopback HTTP.");
            }
            if (rentalAuthority.isBlank() || !rentalAuthority.equals(rentalAuthority.strip())) {
                throw new IllegalArgumentException("Set SEBOARD_RENTAL_AUTHORITY to a SEBoard role name.");
            }
        }
        this.rentalAuthority = rentalAuthority;
        this.json = json;
        this.db = db;
    }

    public boolean enabled() {
        return endpoint != null;
    }

    public Actor authenticate(String token) {
        if (token.length() > 8192 || !token.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")) {
            throw unauthorized();
        }
        long accountId;
        Set<String> authorities = new HashSet<>();
        try {
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(4))
                .header("Authorization", "Bearer " + token).header("Accept", "application/json").GET().build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 401) throw unauthorized();
            if (response.statusCode() == 403) throw new ApiProblem(403, "FORBIDDEN", "SEBoard에서 접근을 거부했습니다.");
            if (response.statusCode() == 404) {
                var error = json.readTree(response.body());
                if (error != null && error.path("code").asInt() == 108) {
                    throw new ApiProblem(401, "TOKEN_EXPIRED", "로그인 토큰이 만료되었습니다.");
                }
            }
            if (response.statusCode() != 200) throw unavailable();

            var body = json.readTree(response.body());
            if (body == null) throw unavailable();
            var id = body.path("accountId");
            var roles = body.path("authorities");
            if (!id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() <= 0 || !roles.isArray()) {
                throw unavailable();
            }
            accountId = id.asLong();
            for (var role : roles) {
                if (!role.isTextual() || role.asText().isBlank()) throw unavailable();
                authorities.add(role.asText());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable();
        } catch (IOException e) {
            throw unavailable();
        }
        boolean admin = authorities.contains("ROLE_ADMIN");
        boolean eligible = admin || authorities.contains(rentalAuthority);
        bind(accountId, eligible);
        return new Actor(accountId, admin ? "ADMIN" : "STUDENT", authorities, eligible);
    }

    private void bind(long accountId, boolean eligible) {
        String subject = Long.toString(accountId);
        var existing = db.queryForList("SELECT issuer,subject FROM external_subject_binding WHERE member_id=?", accountId);
        if (existing.isEmpty()) {
            db.update("""
                INSERT INTO external_subject_binding(member_id,issuer,subject,last_eligibility,last_checked_at)
                VALUES (?,?,?,?,now()) ON CONFLICT DO NOTHING
                """, accountId, ISSUER, subject, eligible);
            existing = db.queryForList("SELECT issuer,subject FROM external_subject_binding WHERE member_id=?", accountId);
        }
        if (existing.size() != 1 || !ISSUER.equals(existing.getFirst().get("issuer"))
                || !subject.equals(existing.getFirst().get("subject"))) {
            throw new ApiProblem(503, "SEBOARD_IDENTITY_CONFLICT", "기존 대여 계정과 SEBoard 계정의 연결을 확인하세요.");
        }
    }

    private static ApiProblem unauthorized() {
        return new ApiProblem(401, "UNAUTHENTICATED", "유효한 SEBoard 로그인 토큰이 필요합니다.");
    }

    private static ApiProblem unavailable() {
        return new ApiProblem(503, "SEBOARD_AUTH_UNAVAILABLE", "SEBoard 인증을 확인할 수 없습니다.");
    }
}
