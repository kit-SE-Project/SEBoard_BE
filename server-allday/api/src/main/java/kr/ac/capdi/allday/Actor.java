package kr.ac.capdi.allday;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;

public record Actor(long memberId, String role, Set<String> authorities, boolean rentalEligible) {
    public Actor(long memberId,String role) {this(memberId,role,Set.of("ROLE_"+role),!role.equals("WORKER"));}
    public Actor { authorities=Set.copyOf(authorities); }
    public boolean admin() { return "ADMIN".equals(role); }
    public static Actor from(HttpServletRequest request) {
        var actor=(Actor)request.getAttribute("actor");
        if (actor==null || "WORKER".equals(actor.role)) throw new ApiProblem(403,"FORBIDDEN","사용자 권한이 필요합니다.");
        return actor;
    }
    public void requireAdmin() { if(!admin()) throw new ApiProblem(403,"FORBIDDEN","관리자 권한이 필요합니다."); }
    public void requireRentalEligibility() {if(!rentalEligible)throw new ApiProblem(403,"RENTAL_NOT_ELIGIBLE","대여 자격이 확인된 회원만 예약할 수 있습니다.");}
    public static String requestId(HttpServletRequest request) { return (String)request.getAttribute("requestId"); }
}
