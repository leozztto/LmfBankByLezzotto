package com.lezztto.LmfBank.auth.service;

import com.lezztto.LmfBank.auth.domain.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_ACCOUNT_ID = "accountId";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    /** Convenience for callers that don't care about authorization (tests, mostly) — USER, no account. */
    public String generateToken(String username) {
        return generateToken(username, Role.USER, null);
    }

    public String generateToken(String username, Role role, Long accountId) {

        var builder = Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(new Date())
                .expiration(
                        new Date(System.currentTimeMillis() + expiration)
                );

        if (accountId != null) {
            builder.claim(CLAIM_ACCOUNT_ID, accountId);
        }

        return builder.signWith(getKey()).compact();
    }

    /** Token lifetime in seconds (the {@code jwt.expiration} property is milliseconds). */
    public long getExpiresInSeconds() {
        return expiration / 1000;
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** Defaults to {@link Role#USER} when the claim is absent — a token minted before ADR 0010. */
    public Role extractRole(String token) {
        String role = parseClaims(token).get(CLAIM_ROLE, String.class);
        return role != null ? Role.valueOf(role) : Role.USER;
    }

    /** {@code null} when the token's user has no account linked yet (or predates ADR 0010). */
    public Long extractAccountId(String token) {
        Number accountId = parseClaims(token).get(CLAIM_ACCOUNT_ID, Number.class);
        return accountId != null ? accountId.longValue() : null;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {

        try {
            extractUsername(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
