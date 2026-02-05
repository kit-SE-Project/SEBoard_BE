package com.seproject.account.common.domain.repository;

import com.seproject.account.common.domain.LoginPreventUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LoginPreventUserRepository extends JpaRepository<LoginPreventUser,Long> {

    //TODO : method name
    @Query(value = "select loginPreventUser from LoginPreventUser loginPreventUser where loginPreventUser.loginId = :loginId and loginPreventUser.localDateTime >= :now")
    Optional<LoginPreventUser> isPreventUser(@Param("loginId") String loginId, @Param("now") LocalDateTime now);
}
