package com.seproject.admin.banned.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.banned.domain.QBannedId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seproject.admin.banned.controller.dto.BannedIdDTO.*;
import static com.seproject.admin.banned.domain.QBannedId.bannedId1;


@RequiredArgsConstructor
@Repository
public class BannedIdQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<BannedIdResponse> findAll(Pageable pageable) {
        List<BannedIdResponse> bannedIds = jpaQueryFactory.select(
                Projections.constructor(BannedIdResponse.class,
                        bannedId1.id, bannedId1.bannedId))
                .from(bannedId1)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = jpaQueryFactory
                .select(bannedId1.count())
                .from(bannedId1)
                .fetchOne();

        return new PageImpl<>(bannedIds,pageable,count);
    }
}
