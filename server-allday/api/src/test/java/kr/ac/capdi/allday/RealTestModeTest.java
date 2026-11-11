package kr.ac.capdi.allday;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class RealTestModeTest {
    @Test void realEquipmentModeCannotStartPresentationWorkersOrSeeds() {
        assertThatThrownBy(()->new RealTestMode(true,true,false,false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new RealTestMode(true,false,true,false)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new RealTestMode(true,false,false,true)).isInstanceOf(IllegalStateException.class);
        assertThat(new RealTestMode(true,false,false,false).name()).isEqualTo("REAL_TEST");
    }
    @Test void primaryModeUsesRealInventoryAndRejectsFixtureWriters() {
        var mode=new RealTestMode(false,false,false,false,true);
        assertThat(mode.enabled).isTrue();assertThat(mode.primary).isTrue();assertThat(mode.name()).isEqualTo("MAIN");
        assertThatThrownBy(()->new RealTestMode(false,true,false,false,true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new RealTestMode(false,false,true,false,true)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new RealTestMode(false,false,false,true,true)).isInstanceOf(IllegalStateException.class);
    }
    @Test void primaryDatabaseMustBeExplicitAndSeparateFromFixtures() {
        assertThatCode(()->DemoEnvironment.validatePrimaryDatabase("capdi_main","capdi_main")).doesNotThrowAnyException();
        for(String fixture:java.util.List.of("capdi_demo","capdi_test","capdi_dev"))
            assertThatThrownBy(()->DemoEnvironment.validatePrimaryDatabase(fixture,fixture)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->DemoEnvironment.validatePrimaryDatabase("other","capdi_main")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->DemoEnvironment.validatePrimaryDatabase("capdi_main","invalid-name")).isInstanceOf(IllegalStateException.class);
    }
}
