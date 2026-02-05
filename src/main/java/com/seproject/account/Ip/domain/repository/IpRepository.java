package com.seproject.account.Ip.domain.repository;

import com.seproject.account.Ip.domain.Ip;
import com.seproject.account.Ip.domain.IpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IpRepository extends JpaRepository<Ip,Long> {
    Optional<Ip> findByIpAddress(String ipAddress);
    boolean existsByIpAddress(String ipAddress);


    @Query("select ip from Ip ip where ip.ipType = :ipType")
    List<Ip> findAllByIpType(@Param("ipType")IpType ipType);

}
