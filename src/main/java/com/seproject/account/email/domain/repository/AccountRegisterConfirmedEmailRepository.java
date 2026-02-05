package com.seproject.account.email.domain.repository;

import com.seproject.account.email.domain.AccountRegisterConfirmedEmail;
import org.springframework.data.repository.CrudRepository;

public interface AccountRegisterConfirmedEmailRepository
        extends CrudRepository<AccountRegisterConfirmedEmail,String> {
}
