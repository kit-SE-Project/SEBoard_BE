package com.seproject.account.token.domain.repository;

import com.seproject.account.token.domain.LogoutLargeRefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface LogoutLargeRefreshTokenRepository extends CrudRepository<LogoutLargeRefreshToken,String> {
}
