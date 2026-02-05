package com.seproject.admin.banned.domain.repository;

import com.seproject.admin.banned.domain.BannedNickname;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BannedNicknameRepository extends JpaRepository<BannedNickname,Long> {

    boolean existsByBannedNickname(String bannedNickname);

    Optional<BannedNickname> findByBannedNickname(String bannedNickname);
}
