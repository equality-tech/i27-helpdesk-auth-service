package com.i27.helpdesk.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProperties {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiryMillis}")
    private long expiryMillis;

    public String getSecret() {
        return secret;
    }

    public long getExpiryMillis() {
        return expiryMillis;
    }
}
