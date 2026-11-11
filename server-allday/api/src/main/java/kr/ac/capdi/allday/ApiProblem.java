package kr.ac.capdi.allday;

public class ApiProblem extends RuntimeException {
    final int status;
    final String code;
    public ApiProblem(int status, String code, String message) { super(message); this.status=status; this.code=code; }
    static ApiProblem invalid(String message) { return new ApiProblem(422,"INVALID_INPUT",message); }
    static ApiProblem conflict(String code,String message) { return new ApiProblem(409,code,message); }
    static ApiProblem notFound() { return new ApiProblem(404,"NOT_FOUND","대상을 찾을 수 없습니다."); }
}
