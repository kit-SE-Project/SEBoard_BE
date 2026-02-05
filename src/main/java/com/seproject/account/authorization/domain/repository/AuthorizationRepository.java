package com.seproject.account.authorization.domain.repository;

import com.seproject.account.authorization.domain.Authorization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuthorizationRepository extends JpaRepository<Authorization,Long>,AuthorizationRepositoryCustom {

    @Query("select distinct data from Authorization data join fetch data.roleAuthorizations order by data.id desc ")
    List<Authorization> findAllAuthorization();
}
