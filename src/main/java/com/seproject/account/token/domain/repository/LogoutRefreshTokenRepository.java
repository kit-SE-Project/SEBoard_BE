package com.seproject.account.token.domain.repository;

import com.seproject.account.token.domain.LogoutRefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface LogoutRefreshTokenRepository extends CrudRepository<LogoutRefreshToken,String> {
}
