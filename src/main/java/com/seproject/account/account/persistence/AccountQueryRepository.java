package com.seproject.account.account.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.account.controller.condition.AccountCondition;
import com.seproject.admin.account.controller.dto.AdminAccountDto;
import com.seproject.board.common.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.seproject.account.account.domain.QAccount.account;
import static com.seproject.member.domain.QMember.member;

@RequiredArgsConstructor
@Repository
public class AccountQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public boolean existsByLoginId(String loginId) {
        return jpaQueryFactory
                .select(account.accountId)
                .from(account)
                .where(eqLoginId(loginId)
                        .and(available()))
                .fetchFirst() != null;
    }

    public boolean existsByNickname(String nickname) {
        return jpaQueryFactory
                .select(member.boardUserId)
                .from(member)
                .leftJoin(member.account, account)
                .where(eqNickname(nickname)
                        .and(available()))
                .fetchFirst() != null;
    }

    public Page<AdminAccountDto.AccountResponse> findAllAccount(AccountCondition condition, Pageable pageable) {
        List<AdminAccountDto.AccountResponse> accounts = jpaQueryFactory
                .select(Projections.constructor(AdminAccountDto.AccountResponse.class,
                        member.account.accountId,
                        member.boardUserId,
                        member.account.loginId,
                        member.account.name,
                        member.name,
                        member.account.createdAt))
                .from(member)
                .join(member.account,account)
                .where(eqStatus(condition.getStatus()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long count = jpaQueryFactory
                .select(account.count())
                .where(eqStatus(condition.getStatus()))
                .from(account)
                .fetchOne();

        return new PageImpl<>(accounts,pageable,count);
    }

    private BooleanExpression eqStatus(Status status) {
        if (status != null) return account.status.eq(status);
        return null;
    }

    private BooleanExpression eqLoginId(String loginId) {
        return account.loginId.eq(loginId);
    }

    private BooleanExpression eqNickname(String nickname) {
        return member.name.eq(nickname);
    }

    private BooleanExpression available() {
        return account.status.ne(Status.PERMANENT_DELETED);
    }
}
