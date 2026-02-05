package com.seproject.account.email.domain;

import org.springframework.data.annotation.Id;

import java.util.UUID;

public abstract class Email {
    @Id
    protected String email;
    protected String authToken;

    private String generateAuthToken() {
        return UUID.randomUUID().toString().substring(0,8);
    }

    public Email(String email) {
        this.email = email;
        this.authToken = generateAuthToken();
    }

    public boolean confirm(String authToken) {
        return this.authToken.equals(authToken);
    }

    public abstract String toMessage();
}
