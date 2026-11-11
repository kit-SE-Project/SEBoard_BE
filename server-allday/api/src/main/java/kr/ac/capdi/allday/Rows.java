package kr.ac.capdi.allday;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;

final class Rows {
    private Rows() {}
    static Map<String,Object> one(JdbcTemplate db,String sql,Object...args) {
        var rows=db.queryForList(sql,args);if(rows.isEmpty()) throw ApiProblem.notFound();return rows.getFirst();
    }
    static long number(Map<String,Object> row,String key) {return ((Number)row.get(key)).longValue();}
    static Instant instant(Map<String,Object> row,String key) {return row.get(key)==null?null:((Timestamp)row.get(key)).toInstant();}
    static Timestamp ts(Instant instant) {return Timestamp.from(instant);}
    static Map<String,Object> api(Map<String,Object> row) {
        var result=new LinkedHashMap<String,Object>();
        row.forEach((key,value)-> {
            StringBuilder name=new StringBuilder();boolean upper=false;
            for(char c:key.toCharArray()) {if(c=='_') upper=true;else {name.append(upper?Character.toUpperCase(c):c);upper=false;}}
            result.put(name.toString(),value instanceof Timestamp time?time.toInstant():value!=null&&value.getClass().getName().equals("org.postgresql.util.PGobject")?value.toString():value);
        });return result;
    }
}
