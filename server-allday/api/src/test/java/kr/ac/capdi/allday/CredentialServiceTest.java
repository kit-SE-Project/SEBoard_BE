package kr.ac.capdi.allday;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;

class CredentialServiceTest {
    final CredentialService service=new CredentialService(new JdbcTemplate(),Base64.getEncoder().encodeToString(new byte[32]));
    @Test void generatesThirteenCharacterPasswords() {
        for(int i=0;i<100;i++)assertThat(service.generate()).matches("[A-Za-z0-9]{13}");
    }
    @Test void encryptsWithDistinctNoncesAndBindsRentalAndVersion() {
        String plain="Abcdefgh12345",a=service.encrypt(plain,1,1),b=service.encrypt(plain,1,1);
        assertThat(a).isNotEqualTo(b).isNotEqualTo(plain);
        assertThat(service.decrypt(a,1,1)).isEqualTo(plain);
        assertThatThrownBy(()->service.decrypt(a,2,1)).isInstanceOf(ApiProblem.class);
        assertThatThrownBy(()->service.decrypt(a,1,2)).isInstanceOf(ApiProblem.class);
        byte[] damaged=Base64.getDecoder().decode(a);damaged[damaged.length-1]^=1;
        assertThatThrownBy(()->service.decrypt(Base64.getEncoder().encodeToString(damaged),1,1)).isInstanceOf(ApiProblem.class);
    }
}
