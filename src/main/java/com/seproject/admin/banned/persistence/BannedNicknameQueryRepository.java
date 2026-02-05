package com.seproject.admin.banned.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.banned.controller.dto.BannedNicknameDTO.BannedNicknameResponse;
import com.seproject.admin.banned.domain.QBannedNickname;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seproject.admin.banned.domain.QBannedNickname.bannedNickname1;


@RequiredArgsConstructor
@Repository
public class BannedNicknameQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<BannedNicknameResponse> findAll(Pageable pageable) {

        List<BannedNicknameResponse> bannedNicknames = jpaQueryFactory
                .select(Projections.constructor(BannedNicknameResponse.class,
                        bannedNickname1.id, bannedNickname1.bannedNickname))
                .from(bannedNickname1)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = jpaQueryFactory
                .select(bannedNickname1.count())
                .from(bannedNickname1)
                .fetchOne();

        return new PageImpl<>(bannedNicknames,pageable,count);
    }
}
