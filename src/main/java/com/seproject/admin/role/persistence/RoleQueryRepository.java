package com.seproject.admin.role.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.account.account.domain.QAccount;
import com.seproject.account.role.domain.QRole;
import com.seproject.account.role.domain.QRoleAccount;
import com.seproject.account.role.domain.RoleAccount;
import com.seproject.admin.role.controller.dto.RoleDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.seproject.account.account.domain.QAccount.*;
import static com.seproject.account.role.domain.QRole.role;
import static com.seproject.account.role.domain.QRoleAccount.*;
import static com.seproject.admin.role.controller.dto.RoleDTO.*;

@Repository
@RequiredArgsConstructor
public class RoleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Map<Long, List<RoleResponse>> findAccountRole(List<Long> accountIds) {

        List<RoleResponse> fetch = jpaQueryFactory
                .select(Projections.constructor(RoleResponse.class,
                        roleAccount.account.accountId,
                        roleAccount.role.roleId,
                        roleAccount.role.name,
                        roleAccount.role.description,
                        roleAccount.role.alias))
                .from(roleAccount)
                .rightJoin(roleAccount.account , account)
                .join(roleAccount.role, role)
                .where(roleAccount.account.accountId.in(accountIds))
                .fetch();

        Map<Long, List<RoleResponse>> collect =
                fetch.stream().collect(Collectors.groupingBy(RoleResponse::getAccountId));

        return collect;
    }
}
