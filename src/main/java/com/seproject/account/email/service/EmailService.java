package com.seproject.account.email.service;

import com.seproject.error.errorCode.ErrorCode;
import com.seproject.error.exception.CustomIllegalArgumentException;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;


public abstract class EmailService {

    @Value("${mail.host}")
    protected String host;

    @Value("${mail.port}")
    protected int port;

    @Value("${mail.username}")
    protected String username;

    @Value("${mail.password}")
    protected String password;

    protected static final String EMAIL_REGEX = "[0-9a-zA-Z]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
    protected static final String KUMOH_EMAIL_REGEX = "\\w+@kumoh.ac.kr";
    public boolean isEmail(String email) {
        return email.matches(EMAIL_REGEX);
    }

    public boolean isKumohMail(String email) {
        return email.matches(KUMOH_EMAIL_REGEX);
    }

    public void send(String email) {
        if(!isEmail(email)) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MAIL,null);
        }

        SimpleEmail simpleEmail = new SimpleEmail();
        simpleEmail.setHostName(host);
        simpleEmail.setSmtpPort(port);
        simpleEmail.setCharset("euc-kr"); // 인코딩 설정(utf-8, euc-kr)
        simpleEmail.setAuthenticator(new DefaultAuthenticator(username, password));
        simpleEmail.setTLS(true);

        try {
            simpleEmail.setFrom(username);

            send(email,simpleEmail);

        } catch (EmailException e) {
            throw new RuntimeException(e);
        }


    }
    protected abstract void send(String email,SimpleEmail simpleEmail) throws EmailException;
    public void confirm(String email,String token) {
        if(!isEmail(email)) {
            throw new CustomIllegalArgumentException(ErrorCode.INVALID_MAIL,null);
        }

        confirmEmail(email,token);
    }

    @Transactional
    protected abstract void confirmEmail(String email,String token);

    public boolean isConfirmed(String email) {
        return isEmail(email) && isConfirmedEmail(email);
    }

    protected abstract boolean isConfirmedEmail(String email);
}
