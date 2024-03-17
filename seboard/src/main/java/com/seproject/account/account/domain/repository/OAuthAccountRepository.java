package com.seproject.account.account.domain.repository;

import com.seproject.account.account.domain.OAuthAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, Long> {

    //TODO : NORMAL OR REPORTED
    @Query("select account from OAuthAccount account where account.loginId = :loginId and account.status = 'NORMAL'")
    Optional<OAuthAccount> findByLoginId(@Param("loginId") String loginId);

    @Query("select account from OAuthAccount account where account.sub = :sub and account.provider = :provider and account.status = 'NORMAL'")
    Optional<OAuthAccount> findOAuthAccountBySubAndProvider(@Param("sub") String sub,@Param("provider") String provider);

    @Modifying
    @Query("update OAuthAccount o set o.sub = 'null' where o.accountId in :accountIds")
    int deleteOAuthAccountByAccountIds(@Param("accountIds") List<Long> accountIds);
}
