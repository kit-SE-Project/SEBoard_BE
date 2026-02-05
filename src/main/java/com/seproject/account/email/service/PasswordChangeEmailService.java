package com.seproject.account.email.service;

import com.seproject.account.account.persistence.AccountQueryRepository;
import com.seproject.account.email.domain.PasswordChangeConfirmedEmail;
import com.seproject.account.email.domain.PasswordChangeEmail;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.email.domain.repository.PasswordChangeConfirmedEmailRepository;
import com.seproject.account.email.domain.repository.PasswordChangeEmailRepository;
import com.seproject.board.common.Status;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import com.seproject.error.exception.CustomUserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@EnableAsync
public class PasswordChangeEmailService extends EmailService {

    private final PasswordChangeEmailRepository passwordChangeEmailRepository;
    private final PasswordChangeConfirmedEmailRepository changeConfirmedEmailRepository;
    private final AccountQueryRepository accountQueryRepository;

    @Override
    protected void send(String email, SimpleEmail simpleEmail) throws EmailException {

        if(!accountQueryRepository.existsByLoginId(email)) {
            throw new CustomUserNotFoundException(ErrorCode.USER_NOT_FOUND,null);
        }

        PasswordChangeEmail passwordChangeEmail = new PasswordChangeEmail(email);
        simpleEmail.setSubject("SE 게시판 비밀번호 변경 이메일 인증");
        simpleEmail.setMsg(passwordChangeEmail.toMessage());
        simpleEmail.addTo(email);

        passwordChangeEmailRepository.save(passwordChangeEmail);
        simpleEmail.send();
    }

    @Override
    protected void confirmEmail(String email, String token) {

        Optional<PasswordChangeEmail> emailOptional = passwordChangeEmailRepository.findById(email);

        if(emailOptional.isEmpty()) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        PasswordChangeEmail passwordChangeEmail = emailOptional.get();

        if(!passwordChangeEmail.confirm(token)) {
            throw new CustomIllegalArgumentException(ErrorCode.INCORRECT_AUTH_CODE,null);
        }

        PasswordChangeConfirmedEmail confirmedEmail = new PasswordChangeConfirmedEmail(email);
        changeConfirmedEmailRepository.save(confirmedEmail);

    }

    @Override
    protected boolean isConfirmedEmail(String email) {
        return changeConfirmedEmailRepository.existsById(email);
    }
}
