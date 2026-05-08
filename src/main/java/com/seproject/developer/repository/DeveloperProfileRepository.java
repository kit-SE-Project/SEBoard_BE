package com.seproject.developer.repository;

import com.seproject.account.account.domain.Account;
import com.seproject.developer.domain.DeveloperProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeveloperProfileRepository extends JpaRepository<DeveloperProfile, Long> {

    Optional<DeveloperProfile> findByAccount(Account account);

    @Query("SELECT dp.account.accountId FROM DeveloperProfile dp " +
           "JOIN dp.skills s WHERE s.skillTag.id IN :skillIds AND dp.account.accountId != :excludeAccountId")
    List<Long> findAccountIdsBySkills(List<Long> skillIds, Long excludeAccountId);
}
