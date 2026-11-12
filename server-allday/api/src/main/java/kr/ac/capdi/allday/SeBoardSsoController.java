package kr.ac.capdi.allday;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SeBoardSsoController {
    static final String START = "/api/v1/sso/start";
    static final String CALLBACK = "/api/v1/sso/callback";
    static final String LOGOUT = "/api/v1/sso/logout";
    private final SeBoardAuthentication sso;

    public SeBoardSsoController(SeBoardAuthentication sso) {
        this.sso = sso;
    }

    @GetMapping(START)
    ResponseEntity<Void> start(HttpServletRequest request) {
        sso.requireEnabled();
        var login = sso.begin(cookie(request, flowCookie()));
        return redirect(login.location()).header(HttpHeaders.SET_COOKIE,
            cookieHeader(flowCookie(), login.browserNonce(), SeBoardAuthentication.FLOW_TTL)).build();
    }

    @GetMapping(CALLBACK)
    ResponseEntity<Void> callback(@RequestParam(required = false) String code,
            @RequestParam(required = false) String state, HttpServletRequest request) {
        sso.requireEnabled();
        try {
            var session = sso.finish(code, state, cookie(request, flowCookie()), sessionCookie(request, sso));
            return redirect(sso.publicLocation(null)).header(HttpHeaders.SET_COOKIE,
                cookieHeader(flowCookie(), "", Duration.ZERO),
                cookieHeader(sessionCookieName(sso), session.token(), session.lifetime())).build();
        } catch (ApiProblem e) {
            return redirect(sso.publicLocation(e.status == 401 ? "expired" : "unavailable")).build();
        } catch (DataAccessException e) {
            return redirect(sso.publicLocation("unavailable")).build();
        }
    }

    @PostMapping(LOGOUT)
    ResponseEntity<Void> logout(HttpServletRequest request) {
        sso.requireOrigin(request.getHeader(HttpHeaders.ORIGIN));
        sso.close(sessionCookie(request, sso));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,
            cookieHeader(sessionCookieName(sso), "", Duration.ZERO)).build();
    }

    @GetMapping("/api/v1/sso/profile")
    Map<String, Object> profile(HttpServletRequest request) {
        sso.requireEnabled();
        return Map.of("memberId", Actor.from(request).memberId(), "nickname", request.getAttribute("seboardNickname"));
    }

    static String sessionCookie(HttpServletRequest request, SeBoardAuthentication sso) {
        return cookie(request, sessionCookieName(sso));
    }

    private String flowCookie() {
        return sso.secureCookies() ? "__Host-allday_flow" : "allday_flow";
    }

    private static String sessionCookieName(SeBoardAuthentication sso) {
        return sso.secureCookies() ? "__Host-allday_session" : "allday_session";
    }

    private String cookieHeader(String name, String value, Duration age) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(sso.secureCookies())
            .sameSite("Lax").path("/").maxAge(age).build().toString();
    }

    private static String cookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        String result = null;
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                if (result != null) return null;
                result = cookie.getValue();
            }
        }
        return result;
    }

    private ResponseEntity.BodyBuilder redirect(URI destination) {
        return ResponseEntity.status(303).location(destination).header("Referrer-Policy", "no-referrer");
    }
}
