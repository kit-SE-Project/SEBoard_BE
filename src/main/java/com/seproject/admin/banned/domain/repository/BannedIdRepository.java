package com.seproject.admin.banned.domain.repository;

import com.seproject.admin.banned.domain.BannedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BannedIdRepository extends JpaRepository<BannedId,Long> {


    Optional<BannedId> findByBannedId(String bannedId);
}
