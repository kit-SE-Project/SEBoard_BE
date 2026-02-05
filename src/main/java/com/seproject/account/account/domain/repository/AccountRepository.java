package com.seproject.account.account.domain.repository;

import com.seproject.account.account.domain.Account;
import com.seproject.board.common.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account,Long> {


    @Query("select a from Account a left join fetch a.roleAccounts ra left join fetch ra.role r where a.loginId = :loginId and a.status = 'NORMAL'")
    Optional<Account> findByLoginIdWithRole(@Param("loginId") String loginId);

    boolean existsByLoginId(String loginId);

    @Modifying
    @Query("update Account a set a.status = :status where a.accountId in :accountIds")
    int deleteAllByIds(@Param("accountIds") List<Long> accountIds, @Param("status") Status status);

    @Modifying
    @Query("update Account a set a.status = 'NORMAL' where a.accountId in :accountIds and a.status = 'TEMP_DELETED'")
    int restoreAllByIds(@Param("accountIds") List<Long> accountIds);
}
