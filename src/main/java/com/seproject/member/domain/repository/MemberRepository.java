package com.seproject.member.domain.repository;

import com.seproject.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member,Long> {
    @Query("select m from Member m where m.account.loginId = :loginId")
    Optional<Member> findByLoginId(@Param("loginId") String loginId);
    @Query("select m from Member m join m.account a where a.accountId = :accountId")
    Optional<Member> findByAccountId(@Param("accountId") Long accountId);

    @Query("select m from Member m join m.account a where m.boardUserId = :boardUserId")
    Optional<Member> findByIdWithAccount(@Param("boardUserId") Long boardUserId);
}
