package kr.ac.capdi.allday;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice
public class ApiErrors {
    static final String MEMBER_RENTAL_LIMIT_MESSAGE="기존 예약을 취소하거나 대여를 반납한 뒤 새로 예약하세요.";
    private static final org.slf4j.Logger log=org.slf4j.LoggerFactory.getLogger(ApiErrors.class);
    static Map<String,Object> body(int status,String code,String message,String requestId) {
        return Map.of("status",status,"code",code,"message",message,"requestId",requestId==null?"unavailable":requestId);
    }
    @ExceptionHandler(ApiProblem.class)
    ResponseEntity<?> problem(ApiProblem e,HttpServletRequest r) {
        return ResponseEntity.status(e.status).body(body(e.status,e.code,e.getMessage(),Actor.requestId(r)));
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<?> media(Exception e,HttpServletRequest r){return ResponseEntity.status(415).body(body(415,"UNSUPPORTED_MEDIA_TYPE","로그인은 폼 인코딩, 나머지 입력은 해당 API의 JSON 형식을 사용하세요.",Actor.requestId(r)));}
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> method(Exception e,HttpServletRequest r){return ResponseEntity.status(405).body(body(405,"METHOD_NOT_ALLOWED","API의 요청 메서드를 확인하세요.",Actor.requestId(r)));}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,MissingRequestHeaderException.class,
        org.springframework.web.bind.MissingServletRequestParameterException.class})
    ResponseEntity<?> input(Exception e,HttpServletRequest r) {
        return ResponseEntity.unprocessableEntity().body(body(422,"INVALID_INPUT","필수 필드·자료형·입력 범위를 확인하세요.",Actor.requestId(r)));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> database(DataAccessException e,HttpServletRequest r) {
        String state="";
        boolean unavailable=e instanceof org.springframework.dao.DataAccessResourceFailureException;
        for(Throwable t=e;t!=null;t=t.getCause()) if(t instanceof SQLException s && s.getSQLState()!=null) {
            String candidate=s.getSQLState();
            if(candidate.startsWith("08")||Set.of("57P01","57P02","57P03","40001","40P01").contains(candidate)) {state=candidate;unavailable=true;break;}
            if(state.isEmpty())state=candidate;
        }
        int status;String code,message;
        if("23P01".equals(state)) {status=409;code="SLOT_CONFLICT";message="다른 예약과 시간이 겹칩니다.";}
        else if("P1010".equals(state)) {status=409;code="MEMBER_RENTAL_LIMIT";message=MEMBER_RENTAL_LIMIT_MESSAGE;}
        else if("23505".equals(state)) {status=409;code="DUPLICATE_RESOURCE";message="이미 등록되었거나 처리 중인 항목입니다.";}
        else if(unavailable) {
            status=503;code="DATABASE_UNAVAILABLE";message="저장소를 사용할 수 없습니다. 잠시 후 다시 시도하세요.";
        } else {status=500;code="DATABASE_ERROR";message="저장 처리에 실패했습니다.";}
        if(status>=500)log.warn("database failure requestId={} sqlState={}",Actor.requestId(r),state);
        return ResponseEntity.status(status).body(body(status,code,message,Actor.requestId(r)));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e,HttpServletRequest r) {
        log.error("unexpected failure requestId={} type={}",Actor.requestId(r),e.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(body(500,"INTERNAL_ERROR","처리 중 오류가 발생했습니다.",Actor.requestId(r)));
    }
}
