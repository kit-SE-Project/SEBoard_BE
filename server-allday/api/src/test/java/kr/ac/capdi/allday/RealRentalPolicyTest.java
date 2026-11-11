package kr.ac.capdi.allday;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RealRentalPolicyTest {
    @Test void defaultDeniesPhysicalEquipmentEvenForAdminButPreservesDemo() {
        var policy=new RealRentalPolicy("");
        policy.require(new Actor(1001,"STUDENT"),Map.of("inventory_source","DEMO"));
        for(String source:new String[]{"MANUAL","AGENT"}) {
            assertThatThrownBy(()->policy.require(new Actor(9001,"ADMIN"),Map.of("inventory_source",source)))
                .isInstanceOfSatisfying(ApiProblem.class,e->assertThat(e.code).isEqualTo("REAL_RENTAL_NOT_APPROVED"));
        }
    }
    @Test void approvalUsesMemberIdAndStillRequiresRentalEligibility() {
        var policy=new RealRentalPolicy("1001, 1002");
        policy.require(new Actor(1001,"STUDENT"),Map.of("inventory_source","MANUAL"));
        assertThatThrownBy(()->policy.require(new Actor(1,"STUDENT"),Map.of("inventory_source","AGENT"))).isInstanceOf(ApiProblem.class);
        assertThatThrownBy(()->policy.require(new Actor(1001,"WORKER"),Map.of("inventory_source","AGENT"))).isInstanceOf(ApiProblem.class);
    }
    @Test void malformedConfigurationCannotEnableAllUsers() {
        for(String value:new String[]{"*","0","-1","1,","1,bad","9223372036854775808"})
            assertThatThrownBy(()->new RealRentalPolicy(value)).isInstanceOf(IllegalArgumentException.class);
    }
}
