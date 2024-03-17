package com.seproject.admin.ip.persitence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.account.Ip.domain.QIp;
import com.seproject.admin.ip.controller.dto.IpDTO;
import com.seproject.admin.ip.controller.dto.IpDTO.IpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seproject.account.Ip.domain.QIp.ip;
import static com.seproject.admin.ip.controller.dto.IpDTO.*;


@RequiredArgsConstructor
@Repository
public class IpQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<IpResponse> findAll(IpCondition condition) {

        List<IpResponse> ips = jpaQueryFactory
                .select(Projections.constructor(IpResponse.class,
                        ip.id, ip.ipAddress,ip.ipType))
                .from(ip)
                .where(ip.ipType.eq(condition.getIpType()))
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
                .fetch();
        return ips;
//        Long count = jpaQueryFactory
//                .select(ip.count())
//                .from(ip)
//                .fetchOne();
//
//        return new PageImpl<>(ips,pageable,count);
    }


}
