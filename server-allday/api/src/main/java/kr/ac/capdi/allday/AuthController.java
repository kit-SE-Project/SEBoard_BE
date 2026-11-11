package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {
    private final DemoAuthService service;
    public AuthController(DemoAuthService service){this.service=service;}
    public record Refresh(@NotBlank @Size(max=8192) String refreshToken){}
    @PostMapping(value="/formLogin",consumes="application/x-www-form-urlencoded")
    public Object login(@RequestParam String username,@RequestParam String password,@RequestParam(defaultValue="false") boolean largeLogin){return service.login(username,password,largeLogin);}
    @PostMapping("/refresh") public Object refresh(@Valid @RequestBody Refresh request){return service.refresh(request.refreshToken());}
    @PostMapping("/logoutProc") public Object logout(@Valid @RequestBody Refresh request,HttpServletRequest r){return service.logout(bearer(r),request.refreshToken());}
    @GetMapping("/mypage") public Object me(HttpServletRequest r){return service.me(bearer(r));}
    static String bearer(HttpServletRequest r){String h=r.getHeader("Authorization");return h!=null&&h.startsWith("Bearer ")?h.substring(7):"";}
}
