package com.seproject.account.token.domain.repository;

import com.seproject.account.token.domain.LogoutToken;
import org.springframework.data.repository.CrudRepository;

public interface LogoutTokenRepository extends CrudRepository<LogoutToken,String> {
}
