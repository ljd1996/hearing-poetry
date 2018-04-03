package com.poetry.hearing.util;

public class TokenDetailImpl implements TokenDetail {

    private final String username;
    private final String password;

    public TokenDetailImpl(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }
}
