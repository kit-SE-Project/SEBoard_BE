package com.seproject.account.email.domain.repository;

import com.seproject.account.email.domain.PasswordChangeEmail;
import org.springframework.data.repository.CrudRepository;

public interface PasswordChangeEmailRepository extends CrudRepository<PasswordChangeEmail,String> {
}
