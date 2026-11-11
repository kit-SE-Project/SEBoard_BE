package kr.ac.capdi.allday;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonCodec {
    private final ObjectMapper mapper;
    public JsonCodec(ObjectMapper mapper) {this.mapper=mapper;}
    public String write(Object value) {try{return mapper.writeValueAsString(value);}catch(Exception e){throw new IllegalStateException("JSON encoding failed",e);}}
    public Object read(String value) {try{return mapper.readValue(value,Object.class);}catch(Exception e){throw new IllegalStateException("JSON decoding failed",e);}}
}
