package kr.ac.capdi.allday;

import java.sql.SQLException;
import org.flywaydb.core.api.callback.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MigrationPermissions implements Callback {
    private final String appRole;
    public MigrationPermissions(@Value("${spring.datasource.username}") String appRole,
        @Value("${spring.flyway.user}") String migrationRole) {
        if(!appRole.matches("[A-Za-z_][A-Za-z0-9_]{0,62}")||appRole.equals(migrationRole))
            throw new IllegalStateException("앱과 마이그레이션은 별도의 DB 역할을 사용해야 합니다.");
        this.appRole=appRole;
    }
    @Override public boolean supports(Event event,Context context){return event==Event.AFTER_MIGRATE;}
    @Override public boolean canHandleInTransaction(Event event,Context context){return true;}
    @Override public void handle(Event event,Context context) {
        try(var statement=context.getConnection().createStatement()) {
            statement.execute("REVOKE INSERT,UPDATE,DELETE ON public.flyway_schema_history FROM \""+appRole+"\"");
            try(var result=statement.executeQuery("SELECT 1 FROM pg_roles WHERE rolname='capdi_worker'")) {
                if(result.next())try(var revoke=context.getConnection().createStatement()){
                    revoke.execute("REVOKE INSERT,UPDATE,DELETE ON public.flyway_schema_history FROM capdi_worker");
                    revoke.execute("REVOKE ALL ON public.accounts,public.roles,public.role_accounts,public.board_users,public.auth_session,public.auth_login_throttle FROM capdi_worker");
                    revoke.execute("REVOKE ALL ON public.allday_sso_login,public.allday_sso_session FROM capdi_worker");
                    revoke.execute("REVOKE ALL ON SEQUENCE public.accounts_account_id_seq,public.roles_role_id_seq,public.board_users_board_user_id_seq FROM capdi_worker");
                }
            }
        }catch(SQLException e){throw new IllegalStateException("마이그레이션 이력 권한 설정 실패");}
    }
    @Override public String getCallbackName(){return "runtime-history-permissions";}
}
