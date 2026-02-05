package com.seproject.admin.bulletin.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.bulletin.controller.dto.BannerDTO.BannerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.seproject.board.bulletin.domain.model.QBanner.banner;

@RequiredArgsConstructor
@Repository
public class AdminBannerQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<BannerResponse> findAllBanner() {
        List<BannerResponse> fetch = jpaQueryFactory
                .select(Projections.constructor(BannerResponse.class, banner))
                .from(banner)
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
                .fetch();

        return fetch;
//
//        Long count = jpaQueryFactory
//                .select(banner.count())
//                .from(banner)
//                .fetchOne();
//
//        return new PageImpl<>(fetch,pageable,count);
    }

    public List<BannerResponse> findActiveBanners(LocalDate currentDate) {
        List<BannerResponse> fetch = jpaQueryFactory
                .select(Projections.constructor(BannerResponse.class, banner))
                .from(banner)
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
                .where(between(currentDate))
                .fetch();
        return fetch;
//        Long count = jpaQueryFactory
//                .select(banner.count())
//                .from(banner)
//                .where(between(currentDate))
//                .fetchOne();
//
//        return new PageImpl<>(fetch,pageable,count);
    }

    public List<BannerResponse> findUnActiveBanners(LocalDate currentDate) {
        List<BannerResponse> fetch = jpaQueryFactory
                .select(Projections.constructor(BannerResponse.class, banner))
                .from(banner)
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize())
                .where(between(currentDate).not())
                .fetch();

        return fetch;

//        Long count = jpaQueryFactory
//                .select(banner.count())
//                .from(banner)
//                .where(between(currentDate).not())
//                .fetchOne();
//
//        return new PageImpl<>(fetch,pageable,count);

    }

    private BooleanExpression between(LocalDate currentDate) {
        return banner.startDate.before(currentDate).and(banner.endDate.after(currentDate));
    }

}


