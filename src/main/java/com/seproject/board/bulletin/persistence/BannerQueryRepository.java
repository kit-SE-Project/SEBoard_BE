package com.seproject.board.bulletin.persistence;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.board.bulletin.controller.dto.BannerDTO.BannerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

import static com.querydsl.core.types.Projections.*;
import static com.seproject.board.bulletin.domain.model.QBanner.banner;

@Repository
@RequiredArgsConstructor
public class BannerQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<BannerResponse> findBanner(LocalDate currentTime) {

        List<BannerResponse> fetch = jpaQueryFactory
                .select(constructor(BannerResponse.class, banner))
                .from(banner)
                .where(between(currentTime))
                .fetch();

        return fetch;
    }

    private BooleanExpression between(LocalDate currentTime) {
        return banner.startDate.loe(currentTime).and(banner.endDate.goe(currentTime));
    }
}
