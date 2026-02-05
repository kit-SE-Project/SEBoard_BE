package com.seproject.account.common.authorize.url;

import com.seproject.account.authorization.domain.Authorization;
import com.seproject.account.authorization.domain.repository.AuthorizationRepository;
import com.seproject.account.authorization.service.AuthorizationService;
import com.seproject.account.role.domain.Role;
import com.seproject.account.role.domain.RoleAuthorization;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;


@Component
public class UrlResourcesFactoryBean implements FactoryBean<LinkedHashMap<RequestMatcher, List<ConfigAttribute>>> {

    private final AuthorizationRepository authorizationRepository;

    public UrlResourcesFactoryBean(AuthorizationRepository authorizationRepository) {
        this.authorizationRepository = authorizationRepository;
    }

    @Override
    public LinkedHashMap<RequestMatcher, List<ConfigAttribute>> getObject() throws Exception {
        LinkedHashMap<RequestMatcher,List<ConfigAttribute>> requestMap = new LinkedHashMap<>();
//        List<Authorization> authorizations = authorizationRepository.findAllAuthorization();
//
//        authorizations.forEach(authorization -> {
//            List<ConfigAttribute> configAttributes = new ArrayList<>();
//            List<Role> roles = authorization.getRoleAuthorizations().stream()
//                    .map(RoleAuthorization::getRole)
//                    .collect(Collectors.toList());
//            roles.forEach(role -> {
//                configAttributes.add(new SecurityConfig(role.getAuthority()));
//            });
//            requestMap.put(new AntPathRequestMatcher(authorization.getPath()),configAttributes);
//        });

        return requestMap;
    }

    @Override
    public Class<?> getObjectType() {
        return null;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }
}
