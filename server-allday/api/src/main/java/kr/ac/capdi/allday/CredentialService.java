package kr.ac.capdi.allday;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CredentialService {
    private static final String ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom random=new SecureRandom();
    private final byte[] key;private final JdbcTemplate db;
    public CredentialService(JdbcTemplate db,@Value("${allday.credential-key}") String encoded) {
        this.db=db;
        try {key=encoded.isEmpty()?new byte[0]:Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException e){throw new IllegalStateException("CREDENTIAL_KEY는 base64여야 합니다.");}
        if(key.length!=0&&key.length!=32) throw new IllegalStateException("CREDENTIAL_KEY는 32바이트 키여야 합니다.");
    }
    private void configured() {if(key.length!=32) throw new ApiProblem(503,"SECRET_NOT_CONFIGURED","비밀 저장 키가 설정되지 않았습니다.");}
    String generate() {
        var b=new StringBuilder();for(int i=0;i<13;i++) b.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));return b.toString();
    }
    String encrypt(String secret,long rentalId,int version) {
        configured();try {
            byte[] nonce=new byte[12];random.nextBytes(nonce);
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,nonce));
            cipher.updateAAD((rentalId+":"+version).getBytes(StandardCharsets.UTF_8));
            byte[] data=cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length+data.length).put(nonce).put(data).array());
        }catch(Exception e){throw new IllegalStateException("Secret encryption failed");}
    }
    String decrypt(String encoded,long rentalId,int version) {
        configured();try {
            byte[] all=Base64.getDecoder().decode(encoded);if(all.length<29)throw new IllegalArgumentException();
            var cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(all,0,12)));
            cipher.updateAAD((rentalId+":"+version).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Arrays.copyOfRange(all,12,all.length)),StandardCharsets.UTF_8);
        }catch(Exception e){throw new ApiProblem(503,"SECRET_UNAVAILABLE","비밀값을 확인할 수 없습니다.");}
    }
    public int createPending(long rentalId) {
        configured();
        Integer next=db.queryForObject("SELECT coalesce(max(credential_version),0)+1 FROM vm_credential WHERE rental_id=?",Integer.class,rentalId);
        String encrypted=encrypt(generate(),rentalId,next);
        db.update("INSERT INTO vm_credential(rental_id,username,credential_version,secret_ref,encrypted_secret,status) VALUES (?,'student',?,?,?,'PENDING')",
            rentalId,next,"db-encrypted:"+rentalId+":"+next,encrypted);return next;
    }
    public Map<String,Object> active(long rentalId) {
        var row=Rows.one(db,"SELECT username,credential_version,encrypted_secret FROM vm_credential WHERE rental_id=? AND status='ACTIVE'",rentalId);
        int version=((Number)row.get("credential_version")).intValue();
        return Map.of("username",row.get("username"),"credentialVersion",version,"password",decrypt(row.get("encrypted_secret").toString(),rentalId,version));
    }
    public Map<String,Object> pending(long rentalId) {
        var row=Rows.one(db,"SELECT username,credential_version,encrypted_secret FROM vm_credential WHERE rental_id=? AND status='PENDING'",rentalId);
        int version=((Number)row.get("credential_version")).intValue();
        return Map.of("username",row.get("username"),"credentialVersion",version,"password",decrypt(row.get("encrypted_secret").toString(),rentalId,version));
    }
}
