package kr.ac.capdi.allday;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class IdempotencyService {
    public record Result(int status,Object body) {}
    private final JdbcTemplate db;private final JsonCodec json;private final TransactionTemplate tx;private final Clock clock;
    public IdempotencyService(JdbcTemplate db,JsonCodec json,PlatformTransactionManager tm,Clock clock) {
        this.db=db;this.json=json;this.tx=new TransactionTemplate(tm);this.clock=clock;
    }
    public Result run(Actor actor,String route,String key,Object payload,Supplier<Result> action) {
        if(key==null||!key.matches("[A-Za-z0-9._:-]{1,128}")) throw ApiProblem.invalid("Idempotency-Key는 1–128자의 영문·숫자·._:- 값이어야 합니다.");
        final String hash;
        try {hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json.write(payload).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e) {throw new IllegalStateException(e);}
        return tx.execute(s -> {
            Timestamp expires=Timestamp.from(clock.instant().plus(Duration.ofHours(24)));
            db.update("INSERT INTO idempotency_record(member_id,route,idem_key,payload_hash,expires_at) VALUES (?,?,?,?,?) ON CONFLICT (member_id,route,idem_key) DO NOTHING",
                actor.memberId(),route,key,hash,expires);
            var row=db.queryForMap("SELECT * FROM idempotency_record WHERE member_id=? AND route=? AND idem_key=? FOR UPDATE",actor.memberId(),route,key);
            long id=((Number)row.get("id")).longValue();
            boolean expired=((Timestamp)row.get("expires_at")).toInstant().isBefore(clock.instant());
            if(expired) db.update("UPDATE idempotency_record SET payload_hash=?,response_status=NULL,response_body=NULL,expires_at=? WHERE id=?",hash,expires,id);
            else {
                if(!hash.equals(row.get("payload_hash"))) throw new ApiProblem(422,"IDEMPOTENCY_KEY_REUSED","같은 키를 다른 요청 내용에 사용할 수 없습니다.");
                if(row.get("response_status")!=null) return new Result(((Number)row.get("response_status")).intValue(),json.read(row.get("response_body").toString()));
            }
            Result result=action.get();
            db.update("UPDATE idempotency_record SET response_status=?,response_body=?::jsonb WHERE id=?",result.status(),json.write(result.body()),id);
            return result;
        });
    }
}
