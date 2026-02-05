package com.seproject.member.domain.repository;

import com.seproject.member.domain.BoardUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardUserRepository extends JpaRepository<BoardUser, Long> {
    @Query("select u from BoardUser u join u.account where u.account.accountId = :accountId")
    List<BoardUser> findByAccountId(Long accountId);
}
