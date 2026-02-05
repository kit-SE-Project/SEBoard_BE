package com.seproject.account.email.service;

import com.seproject.account.account.persistence.AccountQueryRepository;
import com.seproject.account.email.domain.KumohConfirmedEmail;
import com.seproject.account.email.domain.KumohEmail;
import com.seproject.account.account.domain.repository.AccountRepository;
import com.seproject.account.email.domain.repository.KumohConfirmedEmailRepository;
import com.seproject.account.email.domain.repository.KumohEmailRepository;
import com.seproject.board.common.Status;
import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
@EnableAsync
public class KumohEmailService extends EmailService{

    private final KumohEmailRepository kumohEmailRepository;
    private final KumohConfirmedEmailRepository kumohConfirmedEmailRepository;
    private final AccountQueryRepository accountQueryRepository;

    @Async
    @Override
    protected void send(String email, SimpleEmail simpleEmail) throws EmailException {

        if(!isKumohMail(email)){
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MAIL,null);
        }

        if(accountQueryRepository.existsByLoginId(email)) {
            throw new CustomIllegalArgumentException(ErrorCode.USER_ALREADY_EXIST,null);
        }

        KumohEmail kumohEmail = new KumohEmail(email);

        simpleEmail.setSubject("SE 게시판 금오공대 이메일 인증");
        simpleEmail.setMsg(kumohEmail.toMessage());
        simpleEmail.addTo(email);

        kumohEmailRepository.save(kumohEmail);
        simpleEmail.send();
    }

    @Override
    protected void confirmEmail(String email, String token) {
        Optional<KumohEmail> emailOptional = kumohEmailRepository.findById(email);

        if(emailOptional.isEmpty()) {
            throw new CustomIllegalArgumentException(ErrorCode.EMAIL_NOT_FOUNT,null);
        }

        KumohEmail kumohEmail = emailOptional.get();

        if(!kumohEmail.confirm(token)) {
            throw new CustomIllegalArgumentException(ErrorCode.INCORRECT_AUTH_CODE,null);
        }

        KumohConfirmedEmail confirmedEmail = new KumohConfirmedEmail(email);
        kumohConfirmedEmailRepository.save(confirmedEmail);
    }

    @Override
    protected boolean isConfirmedEmail(String email) {
        return kumohConfirmedEmailRepository.existsById(email);
    }
}
