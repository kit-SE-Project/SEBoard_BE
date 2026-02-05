package com.seproject.account.authorization.domain.repository;

import com.seproject.account.authorization.domain.*;

import java.util.Optional;

public interface AuthorizationRepositoryCustom {

    Optional<MenuAccessAuthorization> findMenuAccessAuthorization(Long menuId);
    Optional<MenuManageAuthorization> findMenuManageAuthorization(Long menuId);
    Optional<MenuEditAuthorization> findMenuEditAuthorization(Long menuId);
    Optional<MenuExposeAuthorization> findMenuExposeAuthorization(Long menuId);
}
