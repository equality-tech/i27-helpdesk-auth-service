package com.i27.helpdesk.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";

    @Test
    void generatesSignedTokenWithSubjectAndClaims() {
        JwtProperties properties = new JwtProperties();
        ReflectionTestUtils.setField(properties, "secret", SECRET);
        ReflectionTestUtils.setField(properties, "expiryMillis", 60_000L);
        JwtUtil jwtUtil = new JwtUtil(properties);

        String token = jwtUtil.generateToken(Map.of("role", "ADMIN"), "admin@example.com");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("admin@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }
}
