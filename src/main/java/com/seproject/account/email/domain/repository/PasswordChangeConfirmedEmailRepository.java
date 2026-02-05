package com.seproject.account.email.domain.repository;

import com.seproject.account.email.domain.PasswordChangeConfirmedEmail;
import org.springframework.data.repository.CrudRepository;

public interface PasswordChangeConfirmedEmailRepository
        extends CrudRepository<PasswordChangeConfirmedEmail,String> {
}
