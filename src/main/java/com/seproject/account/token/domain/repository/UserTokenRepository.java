package com.seproject.account.token.domain.repository;

import com.seproject.account.token.domain.UserToken;
import org.springframework.data.repository.CrudRepository;

public interface UserTokenRepository extends CrudRepository<UserToken,String> {
}
