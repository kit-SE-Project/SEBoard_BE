package com.seproject.admin.settings.domain.repository;

import com.seproject.admin.settings.domain.LoginSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginSettingRepository extends JpaRepository<LoginSetting, Long> {


    @Modifying
    @Query("update LoginSetting ls set ls.loginLimitTime = :loginLimitTime, ls.loginTryCount = :loginTryCount")
    int updateAll(@Param("loginLimitTime") long loginLimitTime, @Param("loginTryCount") long loginTryCount);
}
